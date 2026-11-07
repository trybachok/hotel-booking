package com.hotelbooking.hotel.availability;

import jakarta.validation.constraints.NotBlank;

public record ReleaseRequest(@NotBlank String requestId) {}
