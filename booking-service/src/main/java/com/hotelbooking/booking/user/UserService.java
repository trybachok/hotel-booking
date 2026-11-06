package com.hotelbooking.booking.user;

public interface UserService {

  TokenResponse register(RegisterRequest request);

  TokenResponse authenticate(AuthRequest request);

  UserResponse create(UserCreateRequest request);

  UserResponse update(UserUpdateRequest request);

  void delete(long id);
}
