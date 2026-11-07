package com.hotelbooking.hotel.availability;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ConfirmAvailabilityRequest(
    @NotBlank String requestId,
    @NotNull Long bookingId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate) {}
