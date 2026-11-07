package com.hotelbooking.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.common.error.ErrorResponses;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
class GatewayErrorWriter {

  private final ObjectMapper objectMapper;

  GatewayErrorWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    String traceId = exchange.getRequest().getHeaders().getFirst(TraceIdFilter.TRACE_ID);
    byte[] bytes;
    try {
      bytes =
          objectMapper.writeValueAsBytes(
              ErrorResponses.of(
                  status.value(),
                  status.getReasonPhrase(),
                  message,
                  exchange.getRequest().getPath().value(),
                  traceId == null || traceId.isBlank() ? null : traceId));
    } catch (Exception serializationError) {
      bytes =
          ("{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase() + "\"}")
              .getBytes(StandardCharsets.UTF_8);
    }
    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }
}
