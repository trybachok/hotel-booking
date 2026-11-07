package com.hotelbooking.gateway;

import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(JwtGatewayFilter.class);

  private static final List<String> PUBLIC_PREFIXES =
      List.of("/actuator", "/swagger-ui", "/v3/api-docs", "/webjars");

  private final JwtDecoder jwtDecoder;
  private final GatewayErrorWriter errors;

  public JwtGatewayFilter(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.issuer}") String issuer,
      GatewayErrorWriter errors) {
    byte[] keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
    OAuth2TokenValidator<Jwt> validator =
        new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(), new JwtIssuerValidator(issuer));
    decoder.setJwtValidator(validator);
    this.jwtDecoder = decoder;
    this.errors = errors;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (isPublic(exchange.getRequest())) {
      return chain.filter(exchange);
    }
    String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith("Bearer ")) {
      log.info("rejected unauthenticated request path={}", exchange.getRequest().getPath().value());
      return errors.write(exchange, HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    try {
      jwtDecoder.decode(header.substring("Bearer ".length()));
    } catch (JwtException invalid) {
      log.info("rejected invalid token path={}", exchange.getRequest().getPath().value());
      return errors.write(exchange, HttpStatus.UNAUTHORIZED, "Invalid token");
    }
    return chain.filter(exchange);
  }

  private static boolean isPublic(ServerHttpRequest request) {
    String path = request.getPath().value();
    if (request.getMethod() == HttpMethod.POST
        && (path.equals("/user/register") || path.equals("/user/auth"))) {
      return true;
    }
    return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 2;
  }
}
