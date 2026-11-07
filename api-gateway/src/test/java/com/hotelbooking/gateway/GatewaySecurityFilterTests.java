package com.hotelbooking.gateway;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityFilterTests {

  @Autowired private WebTestClient webTestClient;

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.issuer}")
  private String issuer;

  @Test
  void missingTokenIsRejectedAndTraceIdIsGenerated() {
    webTestClient
        .get()
        .uri("/api/hotels")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .value("traceId", not(emptyString()))
        .expectHeader()
        .contentTypeCompatibleWith(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(401)
        .jsonPath("$.message")
        .isEqualTo("Authentication required")
        .jsonPath("$.traceId")
        .exists();
  }

  @Test
  void suppliedTraceIdIsEchoed() {
    webTestClient
        .get()
        .uri("/api/hotels")
        .header("traceId", "trace-from-client")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueEquals("traceId", "trace-from-client")
        .expectBody()
        .jsonPath("$.traceId")
        .isEqualTo("trace-from-client");
  }

  @Test
  void invalidTokenIsRejected() {
    webTestClient
        .get()
        .uri("/api/hotels")
        .header("Authorization", "Bearer not-a-jwt")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Invalid token");
  }

  @Test
  void routeFailureKeepsTheGeneratedTraceId() {
    EntityExchangeResult<byte[]> result =
        webTestClient
            .get()
            .uri("/api/hotels")
            .header("Authorization", "Bearer " + token())
            .exchange()
            .expectStatus()
            .is5xxServerError()
            .expectBody()
            .returnResult();
    String traceId = result.getResponseHeaders().getFirst("traceId");
    assertNotNull(traceId);
    String body = new String(result.getResponseBody(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(body.contains(traceId));
  }

  @Test
  void validTokenPassesTheFilter() {
    webTestClient
        .get()
        .uri("/api/hotels")
        .header("Authorization", "Bearer " + token())
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }

  @Test
  void registrationStaysPublic() {
    webTestClient.post().uri("/user/register").exchange().expectStatus().is5xxServerError();
  }

  private String token() {
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject("gateway-test")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(600))
            .claim("role", "USER")
            .build();
    return new NimbusJwtEncoder(new ImmutableSecret<>(key))
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
