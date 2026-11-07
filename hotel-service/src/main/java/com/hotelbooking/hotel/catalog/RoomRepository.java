package com.hotelbooking.hotel.catalog;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<RoomEntity, Long> {

  boolean existsByHotelIdAndNumber(Long hotelId, String number);

  boolean existsByHotelIdAndNumberAndIdNot(Long hotelId, String number, Long id);

  long countByHotelId(Long hotelId);

  List<RoomEntity> findByHotelIdOrderByIdAsc(Long hotelId);

  @Query("select room from RoomEntity room order by room.hotel.name asc, room.id asc")
  List<RoomEntity> findAllForStatistics();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select room from RoomEntity room where room.id = :id")
  Optional<RoomEntity> findByIdForUpdate(@Param("id") Long id);

  @Query(
      """
      select room from RoomEntity room
      where room.available = true
        and (:hotelId is null or room.hotel.id = :hotelId)
        and not exists (
          select slot.id from RoomLockEntity slot
          where slot.room = room
            and slot.released = false
            and slot.startDate < :end
            and slot.endDate > :start
        )
      order by room.id asc
      """)
  List<RoomEntity> findFree(
      @Param("hotelId") Long hotelId,
      @Param("start") java.time.LocalDate start,
      @Param("end") java.time.LocalDate end);
}
