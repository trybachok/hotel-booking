package com.hotelbooking.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class ErrorDtoAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public ErrorDtoAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    ErrorDtoAuthenticationEntryPoint.write(
        objectMapper, request, response, HttpStatus.FORBIDDEN, "Access denied");
  }
}
