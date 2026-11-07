package com.hotelbooking.booking.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.booking.hotelclient.HotelClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BookingRequestIdRaceTests {

  @LocalServerPort private int port;
  @Autowired private ObjectMapper json;

  @MockitoBean private HotelClient hotel;

  @Test
  void parallelSameRequestIdReturnsOneBooking() throws Exception {
    HttpClient http = HttpClient.newHttpClient();
    String username = "race-" + System.nanoTime();
    send(http, "/user/register", user(username), null);
    String token =
        json.readTree(send(http, "/user/auth", user(username), null).body()).get("token").asText();
    LocalDate start = LocalDate.now().plusDays(12);
    String body =
        "{\"requestId\":\"race-req\",\"roomId\":8,\"autoSelect\":false,\"startDate\":\""
            + start
            + "\",\"endDate\":\""
            + start.plusDays(1)
            + "\"}";
    CountDownLatch go = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<HttpResult> left = pool.submit(() -> booked(http, body, token, go));
      Future<HttpResult> right = pool.submit(() -> booked(http, body, token, go));
      go.countDown();
      HttpResult first = left.get();
      HttpResult second = right.get();
      assertEquals(201, first.status(), first.body());
      assertEquals(201, second.status(), second.body());
      assertEquals(
          json.readTree(first.body()).get("id").asLong(),
          json.readTree(second.body()).get("id").asLong());
    } finally {
      pool.shutdownNow();
    }
    assertEquals(
        1, json.readTree(send(http, "/bookings", null, token).body()).get("totalElements").asInt());
  }

  private HttpResult booked(HttpClient http, String body, String token, CountDownLatch go)
      throws Exception {
    go.await();
    return send(http, "/booking", body, token);
  }

  private static String user(String username) {
    return "{\"username\":\"" + username + "\",\"password\":\"password1\"}";
  }

  private HttpResult send(HttpClient http, String path, String body, String token)
      throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    if (body == null) {
      builder.GET();
    } else {
      builder
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body));
    }
    HttpResponse<String> response =
        http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    return new HttpResult(response.statusCode(), response.body());
  }

  private record HttpResult(int status, String body) {}
}
