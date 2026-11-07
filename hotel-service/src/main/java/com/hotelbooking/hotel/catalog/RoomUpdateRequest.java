package com.hotelbooking.hotel.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomUpdateRequest(
    @NotBlank @Size(max = 20) String number,
    @jakarta.validation.constraints.NotNull Boolean available) {}
