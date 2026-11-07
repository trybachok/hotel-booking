package com.hotelbooking.gateway;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

  static final String TRACE_ID = "traceId";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String incoming = exchange.getRequest().getHeaders().getFirst(TRACE_ID);
    String traceId =
        incoming == null || incoming.isBlank() ? UUID.randomUUID().toString() : incoming;
    ServerHttpRequest request = exchange.getRequest().mutate().header(TRACE_ID, traceId).build();
    exchange.getResponse().getHeaders().set(TRACE_ID, traceId);
    MDC.put(TRACE_ID, traceId);
    return chain
        .filter(exchange.mutate().request(request).build())
        .doFinally(signal -> MDC.remove(TRACE_ID));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
