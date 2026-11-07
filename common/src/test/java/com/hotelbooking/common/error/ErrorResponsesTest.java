package com.hotelbooking.common.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ErrorResponsesTest {

  @Test
  void buildsErrorDtoWithGivenFields() {
    ErrorDTO error = ErrorResponses.of(404, "Not Found", "missing", "/missing", "trace-1");

    assertEquals(404, error.status());
    assertEquals("Not Found", error.error());
    assertEquals("missing", error.message());
    assertEquals("/missing", error.path());
    assertEquals("trace-1", error.traceId());
    assertNotNull(error.timestamp());
  }
}
