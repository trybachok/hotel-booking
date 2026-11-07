package com.hotelbooking.gateway;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class InternalRouteGuardTests {

  @Autowired private WebTestClient webTestClient;

  @Test
  void releaseIsNotPublished() {
    webTestClient
        .get()
        .uri("/api/rooms/7/release")
        .header("traceId", "trace-gateway")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentTypeCompatibleWith(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(404)
        .jsonPath("$.path")
        .isEqualTo("/api/rooms/7/release")
        .jsonPath("$.traceId")
        .isEqualTo("trace-gateway");
  }

  @Test
  void encodedConfirmIsNotPublished() {
    webTestClient
        .post()
        .uri(builder -> builder.path("/api/rooms/7/confirm%2Davailability").build(true))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void matrixReleaseIsNotPublished() {
    webTestClient.post().uri("/api/rooms/7/release;keep=1").exchange().expectStatus().isNotFound();
  }

  @Test
  void confirmAvailabilityIsNotPublished() {
    webTestClient
        .post()
        .uri("/api/rooms/7/confirm-availability")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(404);
  }
}
