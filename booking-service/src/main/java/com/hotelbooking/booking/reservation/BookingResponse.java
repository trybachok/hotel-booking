package com.hotelbooking.booking.reservation;

import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
    Long id,
    Long userId,
    Long roomId,
    LocalDate startDate,
    LocalDate endDate,
    BookingStatus status,
    Instant createdAt,
    String requestId) {}
