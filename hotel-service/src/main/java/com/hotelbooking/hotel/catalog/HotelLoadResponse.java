package com.hotelbooking.hotel.catalog;

import java.util.List;

public record HotelLoadResponse(
    Long hotelId,
    String hotelName,
    int roomCount,
    int availableCount,
    int totalTimesBooked,
    List<RoomResponse> rooms) {}
