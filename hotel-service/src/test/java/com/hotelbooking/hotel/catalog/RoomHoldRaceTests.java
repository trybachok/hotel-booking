package com.hotelbooking.hotel.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hotelbooking.hotel.availability.ConfirmAvailabilityRequest;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class RoomHoldRaceTests {

  @Autowired private HotelCatalogService hotels;
  @Autowired private RoomCatalogService rooms;
  @Autowired private RoomRepository roomRepository;

  @Test
  void parallelConfirmKeepsOneHold() throws Exception {
    long roomId = room("parallel");
    LocalDate start = LocalDate.now().plusDays(30);
    LocalDate end = start.plusDays(2);
    CountDownLatch go = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> left = pool.submit(() -> hold(roomId, "same-request", start, end, go));
      Future<Integer> right = pool.submit(() -> hold(roomId, "same-request", start, end, go));
      go.countDown();
      assertEquals(200, left.get());
      assertEquals(200, right.get());
    } finally {
      pool.shutdownNow();
    }
    assertEquals(1, roomRepository.findById(roomId).orElseThrow().getTimesBooked());
  }

  @Test
  void releaseBeforeConfirmDoesNotHoldTheRoom() {
    long roomId = room("early-release");
    LocalDate start = LocalDate.now().plusDays(40);
    rooms.release(roomId, "late-request");
    ResponseStatusException failure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                rooms.confirm(
                    roomId,
                    new ConfirmAvailabilityRequest("late-request", 4L, start, start.plusDays(1))));
    assertEquals(409, failure.getStatusCode().value());
    assertEquals(0, roomRepository.findById(roomId).orElseThrow().getTimesBooked());
  }

  private int hold(long roomId, String requestId, LocalDate start, LocalDate end, CountDownLatch go)
      throws Exception {
    go.await();
    rooms.confirm(roomId, new ConfirmAvailabilityRequest(requestId, 8L, start, end));
    return 200;
  }

  private long room(String number) {
    long hotelId = hotels.create(new HotelRequest(number + " hotel", "street")).id();
    return rooms.create(new RoomRequest(hotelId, "1", true)).id();
  }
}
