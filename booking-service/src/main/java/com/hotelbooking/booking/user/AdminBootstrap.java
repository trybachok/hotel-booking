package com.hotelbooking.booking.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final String username;
  private final String password;

  public AdminBootstrap(
      UserRepository users,
      PasswordEncoder passwordEncoder,
      @Value("${app.admin.username:}") String username,
      @Value("${app.admin.password:}") String password) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.username = username;
    this.password = password;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (username.isBlank() && password.isBlank()) {
      return;
    }
    if (username.isBlank() || password.isBlank()) {
      throw new IllegalStateException(
          "app.admin.username and app.admin.password must be set together");
    }
    if (password.length() < 8) {
      throw new IllegalStateException("app.admin.password must be at least 8 characters");
    }
    if (users.findByUsername(username).isPresent()) {
      return;
    }
    UserEntity admin = new UserEntity();
    admin.setUsername(username);
    admin.setPassword(passwordEncoder.encode(password));
    admin.setRole(UserRole.ADMIN);
    users.save(admin);
    log.info("admin user created username={}", username);
  }
}
