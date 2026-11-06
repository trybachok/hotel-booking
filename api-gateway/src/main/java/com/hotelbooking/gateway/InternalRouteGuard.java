package com.hotelbooking.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.common.error.ErrorResponses;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Hotel slot confirmation and compensation stay on the service-to-service path. The gateway must
 * not publish them.
 */
@Component
public class InternalRouteGuard implements GlobalFilter, Ordered {

  private static final Pattern INTERNAL =
      Pattern.compile("^/api/rooms/[^/]+/(confirm-availability|release)/?$");

  private final ObjectMapper objectMapper;

  public InternalRouteGuard(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    if (!INTERNAL.matcher(path).matches()) {
      return chain.filter(exchange);
    }
    exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    String traceId = exchange.getRequest().getHeaders().getFirst("traceId");
    byte[] bytes;
    try {
      bytes =
          objectMapper.writeValueAsBytes(
              ErrorResponses.of(
                  HttpStatus.NOT_FOUND.value(),
                  HttpStatus.NOT_FOUND.getReasonPhrase(),
                  "Not Found",
                  path,
                  traceId == null || traceId.isBlank() ? null : traceId));
    } catch (Exception serializationError) {
      bytes = "{\"status\":404,\"error\":\"Not Found\"}".getBytes(StandardCharsets.UTF_8);
    }
    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
