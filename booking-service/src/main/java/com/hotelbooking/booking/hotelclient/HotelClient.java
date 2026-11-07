package com.hotelbooking.booking.hotelclient;

import java.time.LocalDate;
import java.util.List;

public interface HotelClient {

  List<HotelRoom> recommend(LocalDate start, LocalDate end, String bearerToken);

  void confirm(
      long roomId,
      String requestId,
      long bookingId,
      LocalDate start,
      LocalDate end,
      String bearerToken);

  void release(long roomId, String requestId, String bearerToken);
}
