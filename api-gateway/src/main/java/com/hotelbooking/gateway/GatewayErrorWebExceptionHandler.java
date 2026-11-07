package com.hotelbooking.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.common.error.ErrorDTO;
import com.hotelbooking.common.error.ErrorResponses;
import java.nio.charset.StandardCharsets;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GatewayErrorWebExceptionHandler implements WebExceptionHandler {

  private final ObjectMapper objectMapper;

  public GatewayErrorWebExceptionHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
    if (exchange.getResponse().isCommitted()) {
      return Mono.error(exception);
    }
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    String message = "Unexpected error";
    if (exception instanceof ResponseStatusException responseStatus) {
      HttpStatus resolved = HttpStatus.resolve(responseStatus.getStatusCode().value());
      if (resolved != null) {
        status = resolved;
      }
      if (responseStatus.getReason() != null) {
        message = responseStatus.getReason();
      }
    }
    ErrorDTO body =
        ErrorResponses.of(
            status.value(),
            status.getReasonPhrase(),
            message,
            exchange.getRequest().getPath().value(),
            traceId(exchange));
    byte[] bytes;
    try {
      bytes = objectMapper.writeValueAsBytes(body);
    } catch (Exception serializationError) {
      bytes =
          "{\"status\":500,\"error\":\"Internal Server Error\"}".getBytes(StandardCharsets.UTF_8);
    }
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }

  private static String traceId(ServerWebExchange exchange) {
    String header = exchange.getRequest().getHeaders().getFirst(TraceIdFilter.TRACE_ID);
    if (header != null && !header.isBlank()) {
      return header;
    }
    Object attribute = exchange.getAttribute(TraceIdFilter.TRACE_ID);
    if (attribute instanceof String value && !value.isBlank()) {
      return value;
    }
    String response = exchange.getResponse().getHeaders().getFirst(TraceIdFilter.TRACE_ID);
    return response == null || response.isBlank() ? null : response;
  }
}
