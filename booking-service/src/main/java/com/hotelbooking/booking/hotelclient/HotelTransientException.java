package com.hotelbooking.booking.hotelclient;

public class HotelTransientException extends RuntimeException {

  public HotelTransientException(String message) {
    super(message);
  }

  public HotelTransientException(String message, Throwable cause) {
    super(message, cause);
  }
}
