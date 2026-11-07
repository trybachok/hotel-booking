package com.hotelbooking.booking.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserServiceImpl implements UserService {

  private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final UserMapper userMapper;

  public UserServiceImpl(
      UserRepository users,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      UserMapper userMapper) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.userMapper = userMapper;
  }

  @Override
  @Transactional
  public TokenResponse register(RegisterRequest request) {
    UserEntity user = newUser(request.username(), request.password(), UserRole.USER);
    return token(user);
  }

  @Override
  @Transactional(readOnly = true)
  public TokenResponse authenticate(AuthRequest request) {
    UserEntity user =
        users
            .findByUsername(request.username())
            .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
            .orElseThrow(
                () -> {
                  log.warn("authentication failed username={}", request.username());
                  return new ResponseStatusException(
                      HttpStatus.UNAUTHORIZED, "Invalid credentials");
                });
    return token(user);
  }

  @Override
  @Transactional
  public UserResponse create(UserCreateRequest request) {
    UserEntity user = newUser(request.username(), request.password(), request.role());
    return toResponse(user);
  }

  @Override
  @Transactional
  public UserResponse update(UserUpdateRequest request) {
    UserEntity user =
        users
            .findById(request.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    ensureUsernameAvailable(request.username(), user.getId());
    user.setUsername(request.username());
    user.setRole(request.role());
    if (request.password() != null && !request.password().isBlank()) {
      user.setPassword(passwordEncoder.encode(request.password()));
    }
    return toResponse(user);
  }

  @Override
  @Transactional
  public void delete(long id) {
    if (!users.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }
    users.deleteById(id);
  }

  private UserEntity newUser(String username, String password, UserRole role) {
    ensureUsernameAvailable(username, null);
    UserEntity user = new UserEntity();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setRole(role);
    return users.save(user);
  }

  private void ensureUsernameAvailable(String username, Long currentId) {
    users
        .findByUsername(username)
        .filter(existing -> currentId == null || !currentId.equals(existing.getId()))
        .ifPresent(
            existing -> {
              throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
            });
  }

  private TokenResponse token(UserEntity user) {
    return new TokenResponse(jwtService.issue(user), "Bearer", jwtService.expiresInSeconds());
  }

  private UserResponse toResponse(UserEntity user) {
    return userMapper.toResponse(user);
  }
}
