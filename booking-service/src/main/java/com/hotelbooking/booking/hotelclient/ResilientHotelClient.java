package com.hotelbooking.booking.hotelclient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ResilientHotelClient implements HotelClient {

  private final HttpHotelClient http;

  public ResilientHotelClient(HttpHotelClient http) {
    this.http = http;
  }

  @Override
  @CircuitBreaker(name = "hotel")
  @Retry(name = "hotel")
  public List<HotelRoom> recommend(LocalDate start, LocalDate end, String bearerToken) {
    return http.recommend(start, end, bearerToken);
  }

  @Override
  @CircuitBreaker(name = "hotel")
  @Retry(name = "hotel")
  public void confirm(
      long roomId,
      String requestId,
      long bookingId,
      LocalDate start,
      LocalDate end,
      String bearerToken) {
    http.confirm(roomId, requestId, bookingId, start, end, bearerToken);
  }

  @Override
  @CircuitBreaker(name = "hotel")
  @Retry(name = "hotel")
  public void release(long roomId, String requestId, String bearerToken) {
    http.release(roomId, requestId, bearerToken);
  }
}
