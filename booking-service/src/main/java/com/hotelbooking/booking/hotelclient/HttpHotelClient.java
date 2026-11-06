package com.hotelbooking.booking.hotelclient;

import java.time.LocalDate;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class HttpHotelClient {

  private static final ParameterizedTypeReference<List<HotelRoom>> ROOM_LIST =
      new ParameterizedTypeReference<>() {};

  private final RestClient restClient;

  public HttpHotelClient(RestClient hotelRestClient) {
    this.restClient = hotelRestClient;
  }

  public List<HotelRoom> recommend(LocalDate start, LocalDate end, String bearerToken) {
    return exchange(
        () ->
            restClient
                .get()
                .uri(
                    "http://hotel-service/api/rooms/recommend?startDate={start}&endDate={end}",
                    start,
                    end)
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .onStatus(
                    this::isConflict,
                    (request, response) -> {
                      throw new HotelConflictException("No recommended room");
                    })
                .onStatus(
                    HttpStatusCode::is5xxServerError,
                    (request, response) -> {
                      throw new HotelTransientException("Hotel recommend failed");
                    })
                .body(ROOM_LIST));
  }

  public void confirm(
      long roomId,
      String requestId,
      long bookingId,
      LocalDate start,
      LocalDate end,
      String bearerToken) {
    exchange(
        () -> {
          restClient
              .post()
              .uri("http://hotel-service/api/rooms/{id}/confirm-availability", roomId)
              .header("Authorization", "Bearer " + bearerToken)
              .body(new ConfirmBody(requestId, bookingId, start.toString(), end.toString()))
              .retrieve()
              .onStatus(
                  this::isConflict,
                  (request, response) -> {
                    throw new HotelConflictException("Room is not available");
                  })
              .onStatus(
                  HttpStatusCode::is5xxServerError,
                  (request, response) -> {
                    throw new HotelTransientException("Hotel confirm failed");
                  })
              .toBodilessEntity();
          return null;
        });
  }

  public void release(long roomId, String requestId, String bearerToken) {
    exchange(
        () -> {
          restClient
              .post()
              .uri("http://hotel-service/api/rooms/{id}/release", roomId)
              .header("Authorization", "Bearer " + bearerToken)
              .body(new ReleaseBody(requestId))
              .retrieve()
              .onStatus(
                  HttpStatusCode::is5xxServerError,
                  (request, response) -> {
                    throw new HotelTransientException("Hotel release failed");
                  })
              .toBodilessEntity();
          return null;
        });
  }

  private boolean isConflict(HttpStatusCode status) {
    return status.value() == 409;
  }

  private <T> T exchange(Call<T> call) {
    try {
      return call.run();
    } catch (HotelConflictException | HotelTransientException known) {
      throw known;
    } catch (ResourceAccessException timeout) {
      throw new HotelTransientException("Hotel call timed out", timeout);
    }
  }

  private interface Call<T> {
    T run();
  }

  private record ConfirmBody(String requestId, long bookingId, String startDate, String endDate) {}

  private record ReleaseBody(String requestId) {}
}
