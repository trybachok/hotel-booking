package com.hotelbooking.hotel.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<HotelEntity, Long> {

  List<HotelEntity> findAllByOrderByNameAsc();

  List<HotelEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
