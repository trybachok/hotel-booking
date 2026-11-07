package com.hotelbooking.common.web;

import com.hotelbooking.common.error.ErrorDTO;
import com.hotelbooking.common.error.ErrorResponses;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorDTO> handleStatus(
      ResponseStatusException exception, WebRequest request) {
    int code = exception.getStatusCode().value();
    HttpStatus status = HttpStatus.resolve(code);
    String reason = status == null ? "Error" : status.getReasonPhrase();
    String message = exception.getReason() == null ? reason : exception.getReason();
    logStatus(code, path(request), message);
    return ResponseEntity.status(code)
        .body(ErrorResponses.of(code, reason, message, path(request), traceId(request)));
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
    if (message.isBlank()) {
      message = "Validation failed";
    }
    logStatus(HttpStatus.BAD_REQUEST.value(), path(request), message);
    ErrorDTO body =
        ErrorResponses.of(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            path(request),
            traceId(request));
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDTO> handleUnexpected(Exception exception, WebRequest request) {
    ErrorDTO body =
        ErrorResponses.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "Unexpected error",
            path(request),
            traceId(request));
    log.error("status=500 path={} message=Unexpected error", path(request));
    return ResponseEntity.internalServerError().body(body);
  }

  private static void logStatus(int code, String path, String message) {
    if (code >= 500) {
      log.error("status={} path={} message={}", code, path, message);
    } else {
      log.warn("status={} path={} message={}", code, path, message);
    }
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
    logStatus(statusCode.value(), path(request), message);
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
    return TraceIds.current(request);
  }
}
