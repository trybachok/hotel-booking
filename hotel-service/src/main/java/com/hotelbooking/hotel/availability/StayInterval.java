package com.hotelbooking.hotel.availability;

import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A stay is half-open: the checkout date is free for the next guest. */
public final class StayInterval {

  private StayInterval() {}

  public static void requireValid(LocalDate start, LocalDate end) {
    if (start == null || end == null || !start.isBefore(end)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
    }
  }

  public static boolean overlaps(
      LocalDate start, LocalDate end, LocalDate otherStart, LocalDate otherEnd) {
    return start.isBefore(otherEnd) && end.isAfter(otherStart);
  }
}
