package com.hotelbooking.booking.reservation;

import com.hotelbooking.booking.hotelclient.HotelClient;
import com.hotelbooking.booking.hotelclient.HotelConflictException;
import com.hotelbooking.booking.hotelclient.HotelRoom;
import com.hotelbooking.booking.hotelclient.HotelTransientException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingServiceImpl implements BookingService {

  private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);
  private static final int MAX_PAGE_SIZE = 50;

  private final BookingRepository bookings;
  private final HotelClient hotel;
  private final TransactionTemplate transactions;
  private final BookingMapper bookingMapper;

  public BookingServiceImpl(
      BookingRepository bookings,
      HotelClient hotel,
      TransactionTemplate transactions,
      BookingMapper bookingMapper) {
    this.bookings = bookings;
    this.hotel = hotel;
    this.transactions = transactions;
    this.bookingMapper = bookingMapper;
  }

  @Override
  public BookingResponse create(CreateBookingRequest request, long userId, String bearerToken) {
    requireDates(request.startDate(), request.endDate());
    try {
      return createBooking(request, userId, bearerToken);
    } finally {
      MDC.remove("bookingId");
    }
  }

  private BookingResponse createBooking(
      CreateBookingRequest request, long userId, String bearerToken) {
    BookingEntity existing = bookings.findByRequestId(request.requestId()).orElse(null);
    if (existing != null) {
      MDC.put("bookingId", Long.toString(existing.getId()));
      ensureOwner(existing, userId);
      if (existing.getStatus() != BookingStatus.PENDING) {
        return toResponse(existing);
      }
      return confirmOrCompensate(existing, bearerToken, false);
    }
    List<Long> rooms;
    try {
      rooms = candidateRooms(request, bearerToken);
    } catch (RuntimeException failure) {
      if (!isTransient(failure)) {
        throw failure;
      }
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Hotel is unavailable");
    }
    if (rooms.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "No room is available");
    }
    BookingEntity booking = null;
    for (int index = 0; index < rooms.size(); index++) {
      Long roomId = rooms.get(index);
      boolean last = index == rooms.size() - 1 || !request.autoSelect();
      if (overlaps(roomId, request.startDate(), request.endDate(), booking)) {
        if (!request.autoSelect()) {
          throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is already booked");
        }
        continue;
      }
      booking = savePending(booking, userId, roomId, request);
      try {
        return confirmOrCompensate(booking, bearerToken, !last);
      } catch (RoomUnavailable unavailable) {
        if (last) {
          markCancelled(booking.getId());
          throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is not available");
        }
      }
    }
    if (booking != null) {
      markCancelled(booking.getId());
    }
    throw new ResponseStatusException(HttpStatus.CONFLICT, "No room is available");
  }

  @Override
  public BookingPage history(long userId, int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    Page<BookingEntity> result =
        bookings.findByUserId(
            userId, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    return new BookingPage(
        result.stream().map(this::toResponse).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  public BookingResponse get(long id, long userId) {
    BookingEntity booking =
        bookings
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    ensureOwner(booking, userId);
    return toResponse(booking);
  }

  @Override
  public BookingResponse cancel(long id, long userId, String bearerToken) {
    BookingEntity booking =
        bookings
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    ensureOwner(booking, userId);
    MDC.put("bookingId", Long.toString(booking.getId()));
    try {
      if (booking.getStatus() == BookingStatus.CANCELLED) {
        return toResponse(booking);
      }
      releaseQuietly(booking, bearerToken);
      BookingEntity cancelled = markCancelled(booking.getId());
      log.info("bookingId={} status CANCELLED compensation=user-cancel", cancelled.getId());
      return toResponse(cancelled);
    } finally {
      MDC.remove("bookingId");
    }
  }

  private BookingResponse confirmOrCompensate(
      BookingEntity booking, String bearerToken, boolean tryNextOnConflict) {
    try {
      hotel.confirm(
          booking.getRoomId(),
          booking.getRequestId(),
          booking.getId(),
          booking.getStartDate(),
          booking.getEndDate(),
          bearerToken);
      BookingEntity confirmed = markStatus(booking.getId(), BookingStatus.CONFIRMED);
      log.info("bookingId={} status CONFIRMED roomId={}", confirmed.getId(), confirmed.getRoomId());
      return toResponse(confirmed);
    } catch (HotelConflictException conflict) {
      log.info("bookingId={} hotel rejected roomId={}", booking.getId(), booking.getRoomId());
      if (tryNextOnConflict) {
        throw new RoomUnavailable();
      }
      markCancelled(booking.getId());
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is not available");
    } catch (RuntimeException failure) {
      if (!isTransient(failure)) {
        throw failure;
      }
      releaseQuietly(booking, bearerToken);
      markCancelled(booking.getId());
      log.info("bookingId={} status CANCELLED compensation=release", booking.getId());
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Booking was cancelled after hotel call failed");
    }
  }

  private static boolean isTransient(RuntimeException failure) {
    return failure instanceof HotelTransientException
        || failure.getClass().getSimpleName().contains("CallNotPermitted");
  }

  private List<Long> candidateRooms(CreateBookingRequest request, String bearerToken) {
    if (!request.autoSelect()) {
      if (request.roomId() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomId is required");
      }
      return List.of(request.roomId());
    }
    return hotel.recommend(request.startDate(), request.endDate(), bearerToken).stream()
        .map(HotelRoom::id)
        .toList();
  }

  private boolean overlaps(Long roomId, LocalDate start, LocalDate end, BookingEntity current) {
    Long excludeId = current == null ? null : current.getId();
    return bookings.countOverlap(roomId, start, end, excludeId) > 0;
  }

  private BookingEntity savePending(
      BookingEntity current, long userId, long roomId, CreateBookingRequest request) {
    BookingEntity saved =
        transactions.execute(
            status -> {
              BookingEntity booking =
                  current == null
                      ? bookings.findByRequestId(request.requestId()).orElseGet(BookingEntity::new)
                      : bookings.findById(current.getId()).orElseThrow();
              if (booking.getId() == null) {
                booking.setUserId(userId);
                booking.setRequestId(request.requestId());
                booking.setCreatedAt(Instant.now());
                booking.setStartDate(request.startDate());
                booking.setEndDate(request.endDate());
              }
              booking.setRoomId(roomId);
              booking.setStatus(BookingStatus.PENDING);
              return bookings.save(booking);
            });
    log.info("bookingId={} status PENDING roomId={}", saved.getId(), saved.getRoomId());
    MDC.put("bookingId", Long.toString(saved.getId()));
    return saved;
  }

  private BookingEntity markConfirmed(long id) {
    return markStatus(id, BookingStatus.CONFIRMED);
  }

  private BookingEntity markCancelled(long id) {
    return markStatus(id, BookingStatus.CANCELLED);
  }

  private BookingEntity markStatus(long id, BookingStatus status) {
    return transactions.execute(
        tx -> {
          BookingEntity booking = bookings.findById(id).orElseThrow();
          booking.setStatus(status);
          return booking;
        });
  }

  private void releaseQuietly(BookingEntity booking, String bearerToken) {
    try {
      hotel.release(booking.getRoomId(), booking.getRequestId(), bearerToken);
      log.info("bookingId={} compensation release completed", booking.getId());
    } catch (RuntimeException releaseFailure) {
      log.warn("bookingId={} compensation release failed", booking.getId());
    }
  }

  private static void requireDates(LocalDate start, LocalDate end) {
    if (start == null || end == null || !start.isBefore(end) || start.isBefore(LocalDate.now())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "startDate must be before endDate and not in the past");
    }
  }

  private static void ensureOwner(BookingEntity booking, long userId) {
    if (!booking.getUserId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Booking belongs to another user");
    }
  }

  private BookingResponse toResponse(BookingEntity booking) {
    return bookingMapper.toResponse(booking);
  }

  private static final class RoomUnavailable extends RuntimeException {}
}
