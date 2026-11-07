package com.hotelbooking.common.error;

import java.time.Instant;

/** Unified error body for every HTTP service. */
public record ErrorDTO(
    Instant timestamp, int status, String error, String message, String path, String traceId) {}
