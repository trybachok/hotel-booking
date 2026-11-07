package com.hotelbooking.booking.reservation;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

  Optional<BookingEntity> findByRequestId(String requestId);

  boolean existsByUserId(Long userId);

  Page<BookingEntity> findByUserId(Long userId, Pageable pageable);

  @Query(
      """
      select count(booking) from BookingEntity booking
      where booking.roomId = :roomId
        and booking.status in ('PENDING', 'CONFIRMED')
        and (:excludeId is null or booking.id <> :excludeId)
        and booking.startDate < :end
        and booking.endDate > :start
      """)
  long countOverlap(
      @Param("roomId") Long roomId,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end,
      @Param("excludeId") Long excludeId);
}
