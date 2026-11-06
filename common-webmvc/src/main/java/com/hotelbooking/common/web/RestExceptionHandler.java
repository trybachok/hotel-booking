package com.hotelbooking.common.web;

import com.hotelbooking.common.error.ErrorDTO;
import com.hotelbooking.common.error.ErrorResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDTO> handleUnexpected(Exception exception, WebRequest request) {
    ErrorDTO body =
        ErrorResponses.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "Unexpected error",
            path(request),
            traceId(request));
    return ResponseEntity.internalServerError().body(body);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception exception,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    HttpStatus status = HttpStatus.resolve(statusCode.value());
    String reason = status == null ? "Error" : status.getReasonPhrase();
    String message = exception.getMessage() == null ? reason : exception.getMessage();
    ErrorDTO error =
        ErrorResponses.of(statusCode.value(), reason, message, path(request), traceId(request));
    return new ResponseEntity<>(error, headers, statusCode);
  }

  private static String path(WebRequest request) {
    if (request instanceof ServletWebRequest servletRequest) {
      return servletRequest.getRequest().getRequestURI();
    }
    return "";
  }

  private static String traceId(WebRequest request) {
    String traceId = request.getHeader("traceId");
    return traceId == null || traceId.isBlank() ? null : traceId;
  }
}
