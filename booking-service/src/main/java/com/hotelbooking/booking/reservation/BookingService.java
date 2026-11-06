package com.hotelbooking.booking.reservation;

public interface BookingService {

  BookingResponse create(CreateBookingRequest request, long userId, String bearerToken);

  BookingPage history(long userId, int page, int size);

  BookingResponse get(long id, long userId);

  BookingResponse cancel(long id, long userId, String bearerToken);
}
