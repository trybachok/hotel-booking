package com.hotelbooking.common.web;

import java.time.Duration;

public record JwtProperties(String secret, Duration ttl, String issuer) {}
