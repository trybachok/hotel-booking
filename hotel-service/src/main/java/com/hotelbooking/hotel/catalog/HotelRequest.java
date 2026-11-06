package com.hotelbooking.hotel.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HotelRequest(
    @NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 255) String address) {}
