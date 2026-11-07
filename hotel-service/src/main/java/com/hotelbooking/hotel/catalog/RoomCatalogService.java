package com.hotelbooking.hotel.catalog;

import com.hotelbooking.hotel.availability.ConfirmAvailabilityRequest;
import com.hotelbooking.hotel.availability.LockResponse;
import com.hotelbooking.hotel.availability.StayInterval;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoomCatalogService {

  private static final Logger log = LoggerFactory.getLogger(RoomCatalogService.class);

  private final RoomRepository rooms;
  private final RoomLockRepository locks;
  private final HotelCatalogService hotels;
  private final CatalogMapper catalogMapper;

  public RoomCatalogService(
      RoomRepository rooms,
      RoomLockRepository locks,
      HotelCatalogService hotels,
      CatalogMapper catalogMapper) {
    this.rooms = rooms;
    this.locks = locks;
    this.hotels = hotels;
    this.catalogMapper = catalogMapper;
  }

  @Transactional
  public RoomResponse create(RoomRequest request) {
    HotelEntity hotel = hotels.require(request.hotelId());
    if (rooms.existsByHotelIdAndNumber(hotel.getId(), request.number().trim())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Room number already exists");
    }
    RoomEntity room = new RoomEntity();
    room.setHotel(hotel);
    room.setNumber(request.number().trim());
    room.setAvailable(request.available() == null || request.available());
    room.setTimesBooked(0);
    return catalogMapper.toResponse(rooms.save(room));
  }

  @Transactional
  public RoomResponse update(long id, RoomUpdateRequest request) {
    RoomEntity room = require(id);
    String number = request.number().trim();
    if (rooms.existsByHotelIdAndNumberAndIdNot(room.getHotel().getId(), number, id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Room number already exists");
    }
    room.setNumber(number);
    room.setAvailable(request.available());
    return catalogMapper.toResponse(room);
  }

  @Transactional
  public void delete(long id) {
    if (!rooms.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
    }
    if (locks.countByRoomIdAndReleasedFalse(id) > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Room has an active hold");
    }
    locks.deleteByRoomId(id);
    rooms.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<RoomResponse> freeRooms(Long hotelId, LocalDate start, LocalDate end) {
    StayInterval.requireValid(start, end);
    return rooms.findFree(hotelId, start, end).stream().map(catalogMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<RoomResponse> recommend(Long hotelId, LocalDate start, LocalDate end) {
    return freeRooms(hotelId, start, end).stream()
        .sorted(Comparator.comparingInt(RoomResponse::timesBooked).thenComparing(RoomResponse::id))
        .toList();
  }

  @Transactional
  public LockResponse confirm(long roomId, ConfirmAvailabilityRequest request) {
    StayInterval.requireValid(request.startDate(), request.endDate());
    RoomEntity room =
        rooms
            .findByIdForUpdate(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    RoomLockEntity existing = locks.findByRequestId(request.requestId()).orElse(null);
    if (existing != null) {
      return existingHold(roomId, existing);
    }
    if (!room.isAvailable()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is out of service");
    }
    if (!locks.findOverlapping(roomId, request.startDate(), request.endDate()).isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Room is not available for the selected dates");
    }
    RoomLockEntity lock = new RoomLockEntity();
    lock.setRoom(room);
    lock.setBookingId(request.bookingId());
    lock.setRequestId(request.requestId());
    lock.setStartDate(request.startDate());
    lock.setEndDate(request.endDate());
    lock.setReleased(false);
    try {
      locks.saveAndFlush(lock);
    } catch (DataIntegrityViolationException duplicate) {
      RoomLockEntity winner =
          locks
              .findByRequestId(request.requestId())
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.CONFLICT, "Request id is already used"));
      return existingHold(roomId, winner);
    }
    room.setTimesBooked(room.getTimesBooked() + 1);
    MDC.put("bookingId", Long.toString(request.bookingId()));
    try {
      log.info("roomId={} requestId={} hold created", roomId, request.requestId());
    } finally {
      MDC.remove("bookingId");
    }
    return catalogMapper.toLock(lock);
  }

  @Transactional
  public void release(long roomId, String requestId) {
    RoomEntity room = rooms.findByIdForUpdate(roomId).orElse(null);
    RoomLockEntity lock = locks.findByRequestId(requestId).orElse(null);
    if (lock != null) {
      if (!lock.getRoom().getId().equals(roomId)) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Request id belongs to another room");
      }
      markReleased(room, lock);
      return;
    }
    if (room == null) {
      return;
    }
    RoomLockEntity tombstone = new RoomLockEntity();
    tombstone.setRoom(room);
    tombstone.setBookingId(0);
    tombstone.setRequestId(requestId);
    tombstone.setStartDate(LocalDate.of(1970, 1, 1));
    tombstone.setEndDate(LocalDate.of(1970, 1, 1));
    tombstone.setReleased(true);
    try {
      locks.saveAndFlush(tombstone);
    } catch (DataIntegrityViolationException duplicate) {
      RoomLockEntity winner = locks.findByRequestId(requestId).orElseThrow();
      if (!winner.getRoom().getId().equals(roomId)) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Request id belongs to another room");
      }
      markReleased(room, winner);
    }
  }

  private LockResponse existingHold(long roomId, RoomLockEntity existing) {
    if (!existing.getRoom().getId().equals(roomId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Request id is already used");
    }
    if (existing.isReleased()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Request was released");
    }
    return catalogMapper.toLock(existing);
  }

  private void markReleased(RoomEntity room, RoomLockEntity lock) {
    if (lock.isReleased()) {
      return;
    }
    lock.setReleased(true);
    if (room != null && room.getTimesBooked() > 0) {
      room.setTimesBooked(room.getTimesBooked() - 1);
    }
    MDC.put("bookingId", Long.toString(lock.getBookingId()));
    try {
      log.info(
          "roomId={} requestId={} hold released",
          room == null ? null : room.getId(),
          lock.getRequestId());
    } finally {
      MDC.remove("bookingId");
    }
  }

  @Transactional(readOnly = true)
  public List<HotelLoadResponse> statistics(Long hotelId) {
    Map<Long, List<RoomEntity>> grouped = new LinkedHashMap<>();
    for (RoomEntity room : rooms.findAllForStatistics()) {
      if (hotelId != null && !room.getHotel().getId().equals(hotelId)) {
        continue;
      }
      grouped.computeIfAbsent(room.getHotel().getId(), ignored -> new ArrayList<>()).add(room);
    }
    return grouped.values().stream().map(this::toLoad).toList();
  }

  private RoomEntity require(long id) {
    return rooms
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
  }

  private HotelLoadResponse toLoad(List<RoomEntity> hotelRooms) {
    HotelEntity hotel = hotelRooms.get(0).getHotel();
    int available = (int) hotelRooms.stream().filter(RoomEntity::isAvailable).count();
    int booked = hotelRooms.stream().mapToInt(RoomEntity::getTimesBooked).sum();
    List<RoomResponse> responses =
        hotelRooms.stream()
            .sorted(
                Comparator.comparingInt(RoomEntity::getTimesBooked)
                    .reversed()
                    .thenComparing(RoomEntity::getId))
            .map(catalogMapper::toResponse)
            .toList();
    return new HotelLoadResponse(
        hotel.getId(), hotel.getName(), hotelRooms.size(), available, booked, responses);
  }
}
