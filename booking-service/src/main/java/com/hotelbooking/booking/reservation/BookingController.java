package com.hotelbooking.booking.reservation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

  private final BookingService bookings;

  public BookingController(BookingService bookings) {
    this.bookings = bookings;
  }

  @PostMapping("/booking")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public BookingResponse create(
      @Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
    return bookings.create(request, userId(authentication), token(authentication));
  }

  @GetMapping("/bookings")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public BookingPage history(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Authentication authentication) {
    return bookings.history(userId(authentication), page, size);
  }

  @GetMapping("/booking/{id}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public BookingResponse get(@PathVariable long id, Authentication authentication) {
    return bookings.get(id, userId(authentication));
  }

  @DeleteMapping("/booking/{id}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public BookingResponse cancel(@PathVariable long id, Authentication authentication) {
    return bookings.cancel(id, userId(authentication), token(authentication));
  }

  private static long userId(Authentication authentication) {
    Object claim = jwt(authentication).getClaim("uid");
    if (claim instanceof Number number) {
      return number.longValue();
    }
    throw new IllegalStateException("Token does not contain uid");
  }

  private static String token(Authentication authentication) {
    return jwt(authentication).getTokenValue();
  }

  private static Jwt jwt(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
      return jwtAuthentication.getToken();
    }
    throw new IllegalStateException("Authentication is not a JWT");
  }
}
