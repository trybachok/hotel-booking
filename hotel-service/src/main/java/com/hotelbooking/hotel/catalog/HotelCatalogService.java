package com.hotelbooking.hotel.catalog;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HotelCatalogService {

  private final HotelRepository hotels;
  private final RoomRepository rooms;

  public HotelCatalogService(HotelRepository hotels, RoomRepository rooms) {
    this.hotels = hotels;
    this.rooms = rooms;
  }

  @Transactional
  public HotelResponse create(HotelRequest request) {
    HotelEntity hotel = new HotelEntity();
    hotel.setName(request.name().trim());
    hotel.setAddress(request.address().trim());
    return toResponse(hotels.save(hotel));
  }

  @Transactional(readOnly = true)
  public List<HotelResponse> list(String name) {
    List<HotelEntity> found =
        name == null || name.isBlank()
            ? hotels.findAllByOrderByNameAsc()
            : hotels.findByNameContainingIgnoreCaseOrderByNameAsc(name.trim());
    return found.stream().map(HotelCatalogService::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public HotelResponse get(long id) {
    return toResponse(require(id));
  }

  @Transactional
  public HotelResponse update(long id, HotelRequest request) {
    HotelEntity hotel = require(id);
    hotel.setName(request.name().trim());
    hotel.setAddress(request.address().trim());
    return toResponse(hotel);
  }

  @Transactional
  public void delete(long id) {
    if (!hotels.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel not found");
    }
    if (rooms.countByHotelId(id) > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Hotel still has rooms");
    }
    hotels.deleteById(id);
  }

  HotelEntity require(long id) {
    return hotels
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel not found"));
  }

  private static HotelResponse toResponse(HotelEntity hotel) {
    return new HotelResponse(hotel.getId(), hotel.getName(), hotel.getAddress());
  }
}
