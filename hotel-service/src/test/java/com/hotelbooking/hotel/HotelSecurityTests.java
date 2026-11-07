package com.hotelbooking.hotel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.common.web.JwtProperties;
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

@SpringBootTest
@AutoConfigureMockMvc
class HotelSecurityTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtProperties jwtProperties;

  @Test
  void missingTokenIsUnauthorized() throws Exception {
    mockMvc
        .perform(post("/api/hotels").header("traceId", "hotel-trace"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("Authentication required"))
        .andExpect(jsonPath("$.traceId").value("hotel-trace"));
  }

  @Test
  void userCannotCreateHotel() throws Exception {
    mockMvc
        .perform(post("/api/hotels").header("Authorization", "Bearer " + token("USER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void adminPassesAuthorizationAndReachesMissingHandler() throws Exception {
    mockMvc
        .perform(post("/api/hotels").header("Authorization", "Bearer " + token("ADMIN")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void userCanRequestHotelList() throws Exception {
    mockMvc
        .perform(get("/api/hotels").header("Authorization", "Bearer " + token("USER")))
        .andExpect(status().isOk());
  }

  @Test
  void onlyServiceCanHoldARoom() throws Exception {
    String body = holdBody();
    mockMvc
        .perform(
            post("/api/rooms/999999/confirm-availability")
                .header("Authorization", "Bearer " + token("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/rooms/999999/confirm-availability")
                .header("Authorization", "Bearer " + token("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/rooms/999999/release")
                .header("Authorization", "Bearer " + token("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\":\"none\"}"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/rooms/999999/confirm-availability")
                .header("Authorization", "Bearer " + token("SERVICE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void unknownRoleCannotReadHotels() throws Exception {
    mockMvc
        .perform(get("/api/hotels").header("Authorization", "Bearer " + token("SUPERUSER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void tamperedTokenIsRejected() throws Exception {
    String tampered = token("ADMIN") + "x";
    mockMvc
        .perform(get("/api/hotels").header("Authorization", "Bearer " + tampered))
        .andExpect(status().isUnauthorized());
  }

  private static String holdBody() {
    LocalDate start = LocalDate.now().plusDays(20);
    return "{\"requestId\":\"sec-hold\",\"bookingId\":1,\"startDate\":\""
        + start
        + "\",\"endDate\":\""
        + start.plusDays(1)
        + "\"}";
  }

  private String token(String role) {
    SecretKey key =
        new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer())
            .subject("hotel-test")
            .issuedAt(now)
            .expiresAt(now.plus(jwtProperties.ttl()))
            .claim("role", role)
            .build();
    return new NimbusJwtEncoder(new ImmutableSecret<>(key))
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
