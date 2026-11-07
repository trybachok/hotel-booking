package com.hotelbooking.hotel.catalog;

import com.hotelbooking.hotel.availability.ConfirmAvailabilityRequest;
import com.hotelbooking.hotel.availability.LockResponse;
import com.hotelbooking.hotel.availability.ReleaseRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

  private final RoomCatalogService rooms;

  public RoomController(RoomCatalogService rooms) {
    this.rooms = rooms;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public RoomResponse create(@Valid @RequestBody RoomRequest request) {
    return rooms.create(request);
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public RoomResponse update(@PathVariable long id, @Valid @RequestBody RoomUpdateRequest request) {
    return rooms.update(id, request);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    rooms.delete(id);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public List<RoomResponse> free(
      @RequestParam(required = false) Long hotelId,
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate) {
    return rooms.freeRooms(hotelId, startDate, endDate);
  }

  @GetMapping("/recommend")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public List<RoomResponse> recommend(
      @RequestParam(required = false) Long hotelId,
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate) {
    return rooms.recommend(hotelId, startDate, endDate);
  }

  @GetMapping("/statistics")
  @PreAuthorize("hasRole('ADMIN')")
  public List<HotelLoadResponse> statistics(@RequestParam(required = false) Long hotelId) {
    return rooms.statistics(hotelId);
  }

  @PostMapping("/{id}/confirm-availability")
  @PreAuthorize("hasRole('SERVICE')")
  public LockResponse confirm(
      @PathVariable long id, @Valid @RequestBody ConfirmAvailabilityRequest request) {
    return rooms.confirm(id, request);
  }

  @PostMapping("/{id}/release")
  @PreAuthorize("hasRole('SERVICE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void release(@PathVariable long id, @Valid @RequestBody ReleaseRequest request) {
    rooms.release(id, request.requestId());
  }
}
