package com.hotelbooking.booking.reservation;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMapper {

  BookingResponse toResponse(BookingEntity booking);
}
