package com.hotelbooking.booking.hotelclient;

import java.time.LocalDate;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpHotelClient {

  private static final ParameterizedTypeReference<List<HotelRoom>> ROOM_LIST =
      new ParameterizedTypeReference<>() {};

  private final RestClient restClient;
  private final ServiceTokenProvider serviceTokens;

  public HttpHotelClient(RestClient hotelRestClient, ServiceTokenProvider serviceTokens) {
    this.restClient = hotelRestClient;
    this.serviceTokens = serviceTokens;
  }

  public List<HotelRoom> recommend(LocalDate start, LocalDate end, String bearerToken) {
    return exchange(
        () ->
            restClient
                .get()
                .uri("/api/rooms/recommend?startDate={start}&endDate={end}", start, end)
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
              .uri("/api/rooms/{id}/confirm-availability", roomId)
              .header("Authorization", "Bearer " + serviceTokens.serviceToken())
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
              .uri("/api/rooms/{id}/release", roomId)
              .header("Authorization", "Bearer " + serviceTokens.serviceToken())
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
    } catch (RuntimeException failure) {
      HotelConflictException conflict = cause(failure, HotelConflictException.class);
      if (conflict != null) {
        throw conflict;
      }
      HotelTransientException transientFailure = cause(failure, HotelTransientException.class);
      if (transientFailure != null) {
        throw transientFailure;
      }
      if (failure instanceof RestClientException || conversionFailure(failure)) {
        throw new HotelTransientException("Hotel call failed", failure);
      }
      throw failure;
    }
  }

  private static boolean conversionFailure(Throwable failure) {
    Throwable current = failure;
    while (current != null) {
      String name = current.getClass().getName();
      if (name.contains("HttpMessageConversion")
          || name.contains("HttpMessageNotReadable")
          || name.contains("DecodingException")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private static <T extends RuntimeException> T cause(Throwable failure, Class<T> type) {
    Throwable current = failure;
    while (current != null) {
      if (type.isInstance(current)) {
        return type.cast(current);
      }
      current = current.getCause();
    }
    return null;
  }

  private interface Call<T> {
    T run();
  }

  private record ConfirmBody(String requestId, long bookingId, String startDate, String endDate) {}

  private record ReleaseBody(String requestId) {}
}
