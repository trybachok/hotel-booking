package com.hotelbooking.booking.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotelbooking.booking.hotelclient.HotelClient;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class DuplicateRequestIdTests {

  @Autowired private BookingService bookings;

  @MockitoBean private BookingRepository bookingRepository;
  @MockitoBean private HotelClient hotel;

  @Test
  void uniqueRequestIdViolationReturnsTheExistingBooking() {
    LocalDate start = LocalDate.now().plusDays(6);
    LocalDate end = start.plusDays(2);
    BookingEntity existing = new BookingEntity();
    existing.setId(42L);
    existing.setUserId(7L);
    existing.setRoomId(3L);
    existing.setStartDate(start);
    existing.setEndDate(end);
    existing.setStatus(BookingStatus.CONFIRMED);
    existing.setCreatedAt(Instant.parse("2026-11-07T12:00:00Z"));
    existing.setRequestId("req-dup");

    when(bookingRepository.findByRequestId("req-dup"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(existing));
    when(bookingRepository.countOverlap(any(), any(), any(), nullable(Long.class))).thenReturn(0L);
    when(bookingRepository.saveAndFlush(any(BookingEntity.class)))
        .thenThrow(
            new DataIntegrityViolationException(
                "Unique index or primary key violation: \"PUBLIC.UK_BOOKINGS_REQUEST_INDEX_1\""));

    BookingResponse response =
        bookings.create(new CreateBookingRequest("req-dup", 3L, false, start, end), 7L, "token");

    assertEquals(42L, response.id());
    assertEquals(BookingStatus.CONFIRMED, response.status());
    verify(hotel, never()).confirm(anyLong(), anyString(), anyLong(), any(), any(), anyString());
  }

  @Test
  void inFlightDuplicateDoesNotConfirmAgain() {
    LocalDate start = LocalDate.now().plusDays(6);
    LocalDate end = start.plusDays(2);
    BookingEntity existing = new BookingEntity();
    existing.setId(42L);
    existing.setUserId(7L);
    existing.setRoomId(3L);
    existing.setStartDate(start);
    existing.setEndDate(end);
    existing.setStatus(BookingStatus.PENDING);
    existing.setCreatedAt(Instant.parse("2026-11-07T12:00:00Z"));
    existing.setRequestId("req-dup");
    when(bookingRepository.findByRequestId("req-dup"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(existing));
    when(bookingRepository.countOverlap(any(), any(), any(), nullable(Long.class))).thenReturn(0L);
    when(bookingRepository.saveAndFlush(any(BookingEntity.class)))
        .thenThrow(
            new DataIntegrityViolationException(
                "Unique index or primary key violation: \"PUBLIC.UK_BOOKINGS_REQUEST_INDEX_1\""));

    BookingResponse response =
        bookings.create(new CreateBookingRequest("req-dup", 3L, false, start, end), 7L, "token");

    assertEquals(BookingStatus.PENDING, response.status());
    verify(hotel, never()).confirm(anyLong(), anyString(), anyLong(), any(), any(), anyString());
  }
}
