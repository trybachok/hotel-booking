package com.hotelbooking.booking.hotelclient;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpHotelClientTests {

  private final LocalDate start = LocalDate.now().plusDays(3);
  private final LocalDate end = start.plusDays(1);

  @Test
  void notFoundBecomesTransient() {
    Fixture fixture = fixture();
    fixture
        .server
        .expect(requestTo(containsString("/api/rooms/9/confirm-availability")))
        .andExpect(header("Authorization", "Bearer service-token"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThrows(
        HotelTransientException.class,
        () -> fixture.client.confirm(9, "missing", 4, start, end, "token"));
  }

  @Test
  void conflictStaysConflict() {
    Fixture fixture = fixture();
    fixture
        .server
        .expect(requestTo(containsString("/api/rooms/9/confirm-availability")))
        .andExpect(header("Authorization", "Bearer service-token"))
        .andRespond(withStatus(HttpStatus.CONFLICT));

    assertThrows(
        HotelConflictException.class,
        () -> fixture.client.confirm(9, "busy", 4, start, end, "token"));
  }

  @Test
  void unreadableRecommendBecomesTransient() {
    Fixture fixture = fixture();
    fixture
        .server
        .expect(requestTo(containsString("/api/rooms/recommend")))
        .andExpect(header("Authorization", "Bearer token"))
        .andRespond(withSuccess("{", MediaType.APPLICATION_JSON));

    assertThrows(
        HotelTransientException.class, () -> fixture.client.recommend(start, end, "token"));
  }

  private static Fixture fixture() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://hotel");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    return new Fixture(server, new HttpHotelClient(builder.build(), () -> "service-token"));
  }

  private record Fixture(MockRestServiceServer server, HttpHotelClient client) {}
}
