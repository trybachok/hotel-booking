package com.hotelbooking.booking.user;

public record TokenResponse(String token, String tokenType, long expiresInSeconds) {}
