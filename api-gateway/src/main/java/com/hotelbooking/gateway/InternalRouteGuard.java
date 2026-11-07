package com.hotelbooking.gateway;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
    if (!internal(exchange.getRequest().getURI().getRawPath())
        && !internal(exchange.getRequest().getURI().getPath())) {
      return chain.filter(exchange);
    }
    return errors.write(exchange, HttpStatus.NOT_FOUND, "Not Found");
  }

  private static boolean internal(String path) {
    if (path == null || path.isBlank()) {
      return false;
    }
    String decoded = decode(path).replaceAll(";[^/]*", "");
    return INTERNAL.matcher(decoded).matches()
        || INTERNAL.matcher(path.replaceAll(";[^/]*", "")).matches();
  }

  private static String decode(String path) {
    String current = path;
    try {
      for (int attempt = 0; attempt < 2; attempt++) {
        String next = URLDecoder.decode(current, StandardCharsets.UTF_8);
        if (next.equals(current)) {
          return current;
        }
        current = next;
      }
    } catch (IllegalArgumentException invalid) {
      return path;
    }
    return current;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }
}
