package com.hotelbooking.booking.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "hotel.client.base-url=http://127.0.0.1:9",
      "hotel.client.connect-timeout=200ms",
      "hotel.client.read-timeout=200ms"
    })
class BookingCompensationTests {

  @LocalServerPort private int bookingPort;
  @Autowired private ObjectMapper json;

  @Test
  void unreachableHotelCancelsThePendingBooking() throws Exception {
    HttpClient http = HttpClient.newHttpClient();
    String username = "outage-" + System.nanoTime();
    send(
        http,
        "/user/register",
        "{\"username\":\"" + username + "\",\"password\":\"password1\"}",
        null);
    String token =
        json.readTree(
                send(
                        http,
                        "/user/auth",
                        "{\"username\":\"" + username + "\",\"password\":\"password1\"}",
                        null)
                    .body())
            .get("token")
            .asText();
    LocalDate start = LocalDate.now().plusDays(15);
    HttpResult created =
        send(
            http,
            "/booking",
            "{\"requestId\":\"outage-req\",\"roomId\":1,\"autoSelect\":false,\"startDate\":\""
                + start
                + "\",\"endDate\":\""
                + start.plusDays(1)
                + "\"}",
            token);
    assertEquals(503, created.status());
    String status =
        json.readTree(send(http, "/bookings", null, token).body())
            .get("content")
            .get(0)
            .get("status")
            .asText();
    assertEquals("CANCELLED", status);
  }

  private HttpResult send(HttpClient http, String path, String body, String token)
      throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + bookingPort + path));
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
