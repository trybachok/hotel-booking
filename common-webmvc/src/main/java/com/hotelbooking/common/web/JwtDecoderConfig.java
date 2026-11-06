package com.hotelbooking.common.web;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {

  @Bean
  JwtProperties jwtProperties(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.ttl:1h}") Duration ttl,
      @Value("${app.jwt.issuer}") String issuer) {
    return new JwtProperties(secret, ttl, issuer);
  }

  @Bean
  JwtDecoder jwtDecoder(JwtProperties properties) {
    byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
    if (secret.length < 32) {
      throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
    }
    SecretKey key = new SecretKeySpec(secret, "HmacSHA256");
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
    OAuth2TokenValidator<Jwt> validator =
        new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(), new JwtIssuerValidator(properties.issuer()));
    decoder.setJwtValidator(validator);
    return decoder;
  }

  @Bean
  Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
    return new JwtRoleConverter();
  }
}
