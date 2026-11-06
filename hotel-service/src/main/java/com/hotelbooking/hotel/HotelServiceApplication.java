package com.hotelbooking.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.hotelbooking.hotel", "com.hotelbooking.common.web"})
public class HotelServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(HotelServiceApplication.class, args);
  }
}
