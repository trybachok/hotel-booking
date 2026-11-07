package com.hotelbooking.hotel.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomRequest(
    @NotNull Long hotelId, @NotBlank @Size(max = 20) String number, Boolean available) {}
