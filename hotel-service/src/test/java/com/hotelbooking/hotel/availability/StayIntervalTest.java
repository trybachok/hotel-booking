package com.hotelbooking.hotel.availability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StayIntervalTest {

  @Test
  void checkoutDayDoesNotOverlapTheNextStay() {
    LocalDate firstStart = LocalDate.of(2026, 12, 1);
    LocalDate firstEnd = LocalDate.of(2026, 12, 5);
    LocalDate nextStart = LocalDate.of(2026, 12, 5);
    LocalDate nextEnd = LocalDate.of(2026, 12, 8);

    assertFalse(StayInterval.overlaps(firstStart, firstEnd, nextStart, nextEnd));
  }

  @Test
  void overlappingNightsAreDetected() {
    assertTrue(
        StayInterval.overlaps(
            LocalDate.of(2026, 12, 1),
            LocalDate.of(2026, 12, 5),
            LocalDate.of(2026, 12, 4),
            LocalDate.of(2026, 12, 6)));
  }

  @Test
  void endDateMustBeAfterStartDate() {
    assertThrows(
        RuntimeException.class,
        () -> StayInterval.requireValid(LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 5)));
  }
}
