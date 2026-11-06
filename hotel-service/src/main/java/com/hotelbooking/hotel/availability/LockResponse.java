package com.hotelbooking.hotel.availability;

import java.time.LocalDate;

public record LockResponse(
    Long roomId, String requestId, long bookingId, LocalDate startDate, LocalDate endDate) {}
