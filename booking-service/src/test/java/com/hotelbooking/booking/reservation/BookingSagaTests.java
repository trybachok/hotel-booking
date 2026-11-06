package com.hotelbooking.booking.reservation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.booking.hotelclient.HotelClient;
import com.hotelbooking.booking.hotelclient.HotelConflictException;
import com.hotelbooking.booking.hotelclient.HotelRoom;
import com.hotelbooking.booking.hotelclient.HotelTransientException;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class BookingSagaTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private HotelClient hotel;

  @Test
  void confirmsExplicitRoomAndRepeatsTheSameRequest() throws Exception {
    String ada = register("ada-book");
    String body = booking("req-explicit", 15, false);
    MvcResult created =
        mockMvc
            .perform(
                post("/booking")
                    .header("Authorization", "Bearer " + ada)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.roomId").value(15))
            .andReturn();
    Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            post("/booking")
                .header("Authorization", "Bearer " + ada)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id));
    verify(hotel, times(1))
        .confirm(eq(15L), eq("req-explicit"), anyLong(), any(), any(), anyString());

    mockMvc
        .perform(get("/bookings").param("size", "1").header("Authorization", "Bearer " + ada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(id))
        .andExpect(jsonPath("$.totalElements").value(1));

    String other = register("ada-other");
    mockMvc
        .perform(get("/booking/" + id).header("Authorization", "Bearer " + other))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(delete("/booking/" + id).header("Authorization", "Bearer " + ada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    verify(hotel).release(eq(15L), eq("req-explicit"), anyString());
  }

  @Test
  void autoSelectSkipsRejectedRoom() throws Exception {
    String ada = register("ada-auto");
    when(hotel.recommend(any(), any(), anyString())).thenReturn(List.of(room(4), room(9)));
    org.mockito.Mockito.doThrow(new HotelConflictException("busy"))
        .when(hotel)
        .confirm(eq(4L), anyString(), anyLong(), any(), any(), anyString());

    mockMvc
        .perform(
            post("/booking")
                .header("Authorization", "Bearer " + ada)
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking("req-auto", 99, true)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.roomId").value(9))
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
    verify(hotel, never()).release(anyLong(), anyString(), anyString());
  }

  @Test
  void hotelOutageCancelsAndReleases() throws Exception {
    String ada = register("ada-outage");
    org.mockito.Mockito.doThrow(new HotelTransientException("timeout"))
        .when(hotel)
        .confirm(anyLong(), anyString(), anyLong(), any(), any(), anyString());

    mockMvc
        .perform(
            post("/booking")
                .header("Authorization", "Bearer " + ada)
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking("req-outage", 3, false)))
        .andExpect(status().isServiceUnavailable());

    mockMvc
        .perform(get("/bookings").header("Authorization", "Bearer " + ada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].status").value("CANCELLED"));
    verify(hotel).release(eq(3L), eq("req-outage"), anyString());
  }

  @Test
  void secondGuestCannotTakeAConfirmedRoom() throws Exception {
    String first = register("ada-first");
    String second = register("ada-second");
    mockMvc
        .perform(
            post("/booking")
                .header("Authorization", "Bearer " + first)
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking("req-first", 21, false)))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/booking")
                .header("Authorization", "Bearer " + second)
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking("req-second", 21, false)))
        .andExpect(status().isConflict());
    verify(hotel, times(1)).confirm(eq(21L), anyString(), anyLong(), any(), any(), anyString());
  }

  @Test
  void pastDateIsRejected() throws Exception {
    String ada = register("ada-past");
    String yesterday = LocalDate.now().minusDays(1).toString();
    String tomorrow = LocalDate.now().plusDays(1).toString();
    mockMvc
        .perform(
            post("/booking")
                .header("Authorization", "Bearer " + ada)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"requestId\":\"req-past\",\"roomId\":1,\"autoSelect\":false,\"startDate\":\""
                        + yesterday
                        + "\",\"endDate\":\""
                        + tomorrow
                        + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void missingTokenIsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(booking("req-anon", 1, false)))
        .andExpect(status().isUnauthorized());
  }

  private String register(String username) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"" + username + "\",\"password\":\"password1\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
  }

  private static String booking(String requestId, long roomId, boolean autoSelect) {
    LocalDate start = LocalDate.now().plusDays(10);
    return "{\"requestId\":\""
        + requestId
        + "\",\"roomId\":"
        + roomId
        + ",\"autoSelect\":"
        + autoSelect
        + ",\"startDate\":\""
        + start
        + "\",\"endDate\":\""
        + start.plusDays(2)
        + "\"}";
  }

  private static HotelRoom room(long id) {
    return new HotelRoom(id, 1L, String.valueOf(id), true, 0);
  }
}
