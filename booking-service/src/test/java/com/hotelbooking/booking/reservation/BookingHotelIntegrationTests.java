package com.hotelbooking.booking.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.hotel.HotelServiceApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BookingHotelIntegrationTests {

  private static ConfigurableApplicationContext hotel;
  private static int hotelPort;

  @LocalServerPort private int bookingPort;
  @Autowired private ObjectMapper json;

  private final HttpClient http = HttpClient.newHttpClient();

  @DynamicPropertySource
  static void hotelAddress(DynamicPropertyRegistry registry) {
    hotel =
        new SpringApplicationBuilder(HotelServiceApplication.class)
            .run(
                "--server.port=0",
                "--eureka.client.enabled=false",
                "--spring.cloud.discovery.enabled=false",
                "--spring.datasource.url=jdbc:h2:mem:hotel-it;MODE=PostgreSQL",
                "--spring.jpa.hibernate.ddl-auto=none",
                "--spring.sql.init.mode=always");
    hotelPort = ((WebServerApplicationContext) hotel).getWebServer().getPort();
    registry.add("hotel.client.base-url", () -> "http://127.0.0.1:" + hotelPort);
  }

  @AfterAll
  static void stopHotel() {
    if (hotel != null) {
      hotel.close();
    }
  }

  @Test
  void parallelBookingsLeaveOneConfirmedStay() throws Exception {
    String admin = token("admin", "admin12345", false);
    String first = token("parallel-a", "password1", true);
    String second = token("parallel-b", "password1", true);
    long hotelId = createHotel(admin);
    long roomId = createRoom(admin, hotelId, "P1", true);
    LocalDate start = LocalDate.now().plusDays(40);
    LocalDate end = start.plusDays(2);

    CountDownLatch go = new CountDownLatch(1);
    AtomicInteger calls = new AtomicInteger();
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Callable<Integer> book =
          () -> {
            int call = calls.getAndIncrement();
            go.await();
            String bearer = call % 2 == 0 ? first : second;
            return post(
                    bookingPort,
                    "/booking",
                    bookingBody("parallel-" + call, roomId, start, end),
                    bearer)
                .status();
          };
      Future<Integer> left = pool.submit(book);
      Future<Integer> right = pool.submit(book);
      go.countDown();
      List<Integer> statuses = List.of(left.get(), right.get());
      assertTrue(statuses.contains(201), statuses.toString());
      assertTrue(statuses.contains(409), statuses.toString());
    } finally {
      pool.shutdownNow();
    }

    assertEquals(1, confirmedCount(first) + confirmedCount(second));
  }

  @Test
  void unavailableRoomCancelsTheBooking() throws Exception {
    String admin = token("admin", "admin12345", false);
    String guest = token("repair-guest", "password1", true);
    long hotelId = createHotel(admin);
    long roomId = createRoom(admin, hotelId, "R1", false);
    LocalDate start = LocalDate.now().plusDays(50);
    HttpResult result =
        post(
            bookingPort,
            "/booking",
            bookingBody("repair-req", roomId, start, start.plusDays(1)),
            guest);
    assertEquals(409, result.status());
    assertEquals("CANCELLED", onlyStatus(guest));
  }

  private int confirmedCount(String bearer) throws Exception {
    JsonNode content = json.readTree(get(bookingPort, "/bookings", bearer).body()).get("content");
    int confirmed = 0;
    for (JsonNode booking : content) {
      if ("CONFIRMED".equals(booking.get("status").asText())) {
        confirmed++;
      }
    }
    return confirmed;
  }

  private String onlyStatus(String bearer) throws Exception {
    return json.readTree(get(bookingPort, "/bookings", bearer).body())
        .get("content")
        .get(0)
        .get("status")
        .asText();
  }

  private String token(String username, String password, boolean register) throws Exception {
    if (register) {
      post(
          bookingPort,
          "/user/register",
          "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}",
          null);
    }
    return json.readTree(
            post(
                    bookingPort,
                    "/user/auth",
                    "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}",
                    null)
                .body())
        .get("token")
        .asText();
  }

  private long createHotel(String admin) throws Exception {
    HttpResult result =
        post(
            hotelPort,
            "/api/hotels",
            "{\"name\":\"IT " + System.nanoTime() + "\",\"address\":\"IT street\"}",
            admin);
    assertEquals(201, result.status());
    return json.readTree(result.body()).get("id").asLong();
  }

  private long createRoom(String admin, long hotelId, String number, boolean available)
      throws Exception {
    HttpResult result =
        post(
            hotelPort,
            "/api/rooms",
            "{\"hotelId\":"
                + hotelId
                + ",\"number\":\""
                + number
                + "\",\"available\":"
                + available
                + "}",
            admin);
    assertEquals(201, result.status());
    return json.readTree(result.body()).get("id").asLong();
  }

  private static String bookingBody(String requestId, long roomId, LocalDate start, LocalDate end) {
    return "{\"requestId\":\""
        + requestId
        + "\",\"roomId\":"
        + roomId
        + ",\"autoSelect\":false,\"startDate\":\""
        + start
        + "\",\"endDate\":\""
        + end
        + "\"}";
  }

  private HttpResult get(int port, String path, String token) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    HttpResponse<String> response =
        http.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
    return new HttpResult(response.statusCode(), response.body());
  }

  private HttpResult post(int port, String path, String body, String token) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body));
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    HttpResponse<String> response =
        http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    return new HttpResult(response.statusCode(), response.body());
  }

  private record HttpResult(int status, String body) {}
}
