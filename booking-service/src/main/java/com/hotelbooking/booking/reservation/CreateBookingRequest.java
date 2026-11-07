package com.hotelbooking.booking.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateBookingRequest(
    @NotBlank String requestId,
    Long roomId,
    @NotNull Boolean autoSelect,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate) {}
