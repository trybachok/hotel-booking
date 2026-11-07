package com.hotelbooking.booking.hotelclient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class HotelRetryTests {

  @Autowired private HotelClient hotel;

  @MockitoBean private HttpHotelClient http;

  @Test
  void transientConfirmIsRetried() {
    LocalDate start = LocalDate.now().plusDays(3);
    LocalDate end = start.plusDays(1);
    doThrow(new HotelTransientException("timeout"))
        .when(http)
        .confirm(1, "retry", 8, start, end, "token");

    assertThrows(
        HotelTransientException.class, () -> hotel.confirm(1, "retry", 8, start, end, "token"));

    verify(http, times(3)).confirm(1, "retry", 8, start, end, "token");
  }
}
