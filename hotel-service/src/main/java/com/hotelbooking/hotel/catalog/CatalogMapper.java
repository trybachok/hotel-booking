package com.hotelbooking.hotel.catalog;

import com.hotelbooking.hotel.availability.LockResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogMapper {

  HotelResponse toResponse(HotelEntity hotel);

  @Mapping(target = "hotelId", source = "hotel.id")
  RoomResponse toResponse(RoomEntity room);

  @Mapping(target = "roomId", source = "room.id")
  LockResponse toLock(RoomLockEntity lock);
}
