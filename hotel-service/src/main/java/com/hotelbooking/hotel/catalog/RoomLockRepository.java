package com.hotelbooking.hotel.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomLockRepository extends JpaRepository<RoomLockEntity, Long> {

  Optional<RoomLockEntity> findByRequestId(String requestId);

  long countByRoomId(Long roomId);

  @Query(
      """
      select slot from RoomLockEntity slot
      where slot.room.id = :roomId
        and slot.startDate < :end
        and slot.endDate > :start
      """)
  List<RoomLockEntity> findOverlapping(
      @Param("roomId") Long roomId,
      @Param("start") java.time.LocalDate start,
      @Param("end") java.time.LocalDate end);
}
