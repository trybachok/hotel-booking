package com.hotelbooking.hotel.catalog;

public record RoomResponse(
    Long id, Long hotelId, String number, boolean available, int timesBooked) {}
