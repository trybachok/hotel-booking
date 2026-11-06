package com.hotelbooking.booking.hotelclient;

public record HotelRoom(Long id, Long hotelId, String number, boolean available, int timesBooked) {}
