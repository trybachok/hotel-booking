package com.hotelbooking.hotel.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.common.web.JwtProperties;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class HotelCatalogTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtProperties jwtProperties;

  @Test
  void adminManagesHotelAndRoom() throws Exception {
    String admin = token("ADMIN");
    MvcResult created =
        mockMvc
            .perform(
                post("/api/hotels")
                    .header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test Inn\",\"address\":\"Test street 1\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Inn"))
            .andReturn();
    Number hotelId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            patch("/api/hotels/" + hotelId)
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Inn Updated\",\"address\":\"Test street 2\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Inn Updated"));

    mockMvc
        .perform(
            get("/api/hotels")
                .param("name", "Updated")
                .header("Authorization", "Bearer " + token("USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(hotelId));

    MvcResult room =
        mockMvc
            .perform(
                post("/api/rooms")
                    .header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"hotelId\":" + hotelId + ",\"number\":\"12A\",\"available\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.timesBooked").value(0))
            .andReturn();
    Number roomId = JsonPath.read(room.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            post("/api/rooms")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hotelId\":" + hotelId + ",\"number\":\"12A\",\"available\":true}"))
        .andExpect(status().isConflict());

    mockMvc
        .perform(delete("/api/rooms/" + roomId).header("Authorization", "Bearer " + admin))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(delete("/api/hotels/" + hotelId).header("Authorization", "Bearer " + admin))
        .andExpect(status().isNoContent());
  }

  @Test
  void recommendSortsFreeRoomsByTimesBookedThenId() throws Exception {
    String admin = token("ADMIN");
    String user = token("USER");
    Number hotelId = createHotel(admin, "Sort Hotel", "Sort street");
    Number busy = createRoom(admin, hotelId, "A");
    Number quiet = createRoom(admin, hotelId, "B");
    Number repair = createRoom(admin, hotelId, "C");
    mockMvc
        .perform(
            patch("/api/rooms/" + repair)
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"number\":\"C\",\"available\":false}"))
        .andExpect(status().isOk());
    confirm(admin, busy, "busy-later", 1, "2026-12-10", "2026-12-12");

    String start = LocalDate.of(2026, 12, 1).toString();
    String end = LocalDate.of(2026, 12, 3).toString();
    mockMvc
        .perform(
            get("/api/rooms/recommend")
                .param("hotelId", hotelId.toString())
                .param("startDate", start)
                .param("endDate", end)
                .header("Authorization", "Bearer " + user))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(quiet))
        .andExpect(jsonPath("$[1].id").value(busy));

    mockMvc
        .perform(
            get("/api/rooms")
                .param("hotelId", hotelId.toString())
                .param("startDate", start)
                .param("endDate", end)
                .header("Authorization", "Bearer " + user))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(busy))
        .andExpect(jsonPath("$[1].id").value(quiet));
  }

  @Test
  void confirmIsIdempotentAndReleaseFreesTheRoom() throws Exception {
    String admin = token("ADMIN");
    Number hotelId = createHotel(admin, "Hold Hotel", "Hold street");
    Number roomId = createRoom(admin, hotelId, "1");
    confirm(admin, roomId, "req-1", 10);
    mockMvc
        .perform(
            post("/api/rooms/" + roomId + "/confirm-availability")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmBody("req-1", 10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestId").value("req-1"));

    String start = LocalDate.of(2026, 12, 1).toString();
    String end = LocalDate.of(2026, 12, 4).toString();
    mockMvc
        .perform(
            get("/api/rooms")
                .param("hotelId", hotelId.toString())
                .param("startDate", start)
                .param("endDate", end)
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    mockMvc
        .perform(
            post("/api/rooms/" + roomId + "/confirm-availability")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmBody("req-2", 11)))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            post("/api/rooms/" + roomId + "/release")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\":\"req-1\"}"))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            post("/api/rooms/" + roomId + "/release")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\":\"req-1\"}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/rooms/recommend")
                .param("hotelId", hotelId.toString())
                .param("startDate", start)
                .param("endDate", end)
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].timesBooked").value(0));
  }

  @Test
  void statisticsAreVisibleToAdminOnly() throws Exception {
    mockMvc
        .perform(get("/api/rooms/statistics").header("Authorization", "Bearer " + token("USER")))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/rooms/statistics").header("Authorization", "Bearer " + token("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.hotelName == 'Астория')].totalTimesBooked").value(7))
        .andExpect(jsonPath("$[?(@.hotelName == 'Астория')].availableCount").value(2));
  }

  private void confirm(String admin, Number roomId, String requestId, long bookingId)
      throws Exception {
    confirm(admin, roomId, requestId, bookingId, "2026-12-01", "2026-12-04");
  }

  private void confirm(
      String admin, Number roomId, String requestId, long bookingId, String start, String end)
      throws Exception {
    mockMvc
        .perform(
            post("/api/rooms/" + roomId + "/confirm-availability")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmBody(requestId, bookingId, start, end)))
        .andExpect(status().isOk());
  }

  private static String confirmBody(String requestId, long bookingId) {
    return confirmBody(requestId, bookingId, "2026-12-01", "2026-12-04");
  }

  private static String confirmBody(String requestId, long bookingId, String start, String end) {
    return "{\"requestId\":\""
        + requestId
        + "\",\"bookingId\":"
        + bookingId
        + ",\"startDate\":\""
        + start
        + "\",\"endDate\":\""
        + end
        + "\"}";
  }

  private Number createHotel(String admin, String name, String address) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/hotels")
                    .header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + name + "\",\"address\":\"" + address + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private Number createRoom(String admin, Number hotelId, String number) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/rooms")
                    .header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"hotelId\":"
                            + hotelId
                            + ",\"number\":\""
                            + number
                            + "\",\"available\":true}"))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private String token(String role) {
    SecretKey key =
        new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer())
            .subject("hotel-catalog")
            .issuedAt(now)
            .expiresAt(now.plus(jwtProperties.ttl()))
            .claim("role", role)
            .build();
    return new NimbusJwtEncoder(new ImmutableSecret<>(key))
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
