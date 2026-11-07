package com.hotelbooking.booking.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

  private final UserService users;

  public UserController(UserService users) {
    this.users = users;
  }

  @PostMapping("/user/register")
  @ResponseStatus(HttpStatus.CREATED)
  public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
    return users.register(request);
  }

  @PostMapping("/user/auth")
  public TokenResponse authenticate(@Valid @RequestBody AuthRequest request) {
    return users.authenticate(request);
  }

  @PostMapping("/user")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
    return users.create(request);
  }

  @PatchMapping("/user")
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse update(@Valid @RequestBody UserUpdateRequest request) {
    return users.update(request);
  }

  @DeleteMapping("/user")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@RequestParam long id) {
    users.delete(id);
  }
}
