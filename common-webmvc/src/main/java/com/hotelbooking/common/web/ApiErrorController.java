package com.hotelbooking.common.web;

import com.hotelbooking.common.error.ErrorDTO;
import com.hotelbooking.common.error.ErrorResponses;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {

  @RequestMapping("/error")
  public ResponseEntity<ErrorDTO> error(HttpServletRequest request) {
    int status = status(request);
    HttpStatus httpStatus = HttpStatus.resolve(status);
    String reason = httpStatus == null ? "Error" : httpStatus.getReasonPhrase();
    Object rawMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
    String message = rawMessage instanceof String text && !text.isBlank() ? text : reason;
    Object rawPath = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    String path = rawPath instanceof String text ? text : request.getRequestURI();
    ErrorDTO body = ErrorResponses.of(status, reason, message, path, traceId(request));
    return ResponseEntity.status(status).body(body);
  }

  private static int status(HttpServletRequest request) {
    Object raw = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    if (raw instanceof Integer code) {
      return code;
    }
    return HttpStatus.INTERNAL_SERVER_ERROR.value();
  }

  private static String traceId(HttpServletRequest request) {
    String traceId = request.getHeader("traceId");
    return traceId == null || traceId.isBlank() ? null : traceId;
  }
}
