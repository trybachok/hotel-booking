package com.hotelbooking.booking.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @NotNull Long id,
    @NotBlank @Size(min = 3, max = 50) String username,
    @Size(min = 8, max = 100) String password,
    @NotNull UserRole role) {}
