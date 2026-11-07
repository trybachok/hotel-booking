package com.hotelbooking.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.common.error.ErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class ErrorDtoAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public ErrorDtoAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    write(objectMapper, request, response, HttpStatus.UNAUTHORIZED, "Authentication required");
  }

  static void write(
      ObjectMapper objectMapper,
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        ErrorResponses.of(
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            TraceIds.current(request)));
  }
}
