package com.hotelbooking.gateway;

import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
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

  private final GatewayErrorWriter errors;

  public InternalRouteGuard(GatewayErrorWriter errors) {
    this.errors = errors;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    if (!INTERNAL.matcher(path).matches()) {
      return chain.filter(exchange);
    }
    return errors.write(exchange, HttpStatus.NOT_FOUND, "Not Found");
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }
}
