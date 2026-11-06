package com.hotelbooking.common.error;

import java.time.Instant;

public final class ErrorResponses {

  private ErrorResponses() {}

  public static ErrorDTO of(int status, String error, String message, String path, String traceId) {
    return new ErrorDTO(Instant.now(), status, error, message, path, traceId);
  }
}
