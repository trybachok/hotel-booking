package com.hotelbooking.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
    scanBasePackages = {"com.hotelbooking.hotel", "com.hotelbooking.common.web"},
    exclude = UserDetailsServiceAutoConfiguration.class)
public class HotelServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(HotelServiceApplication.class, args);
  }
}
