package com.hotelbooking.hotel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.common.web.ErrorDtoAccessDeniedHandler;
import com.hotelbooking.common.web.ErrorDtoAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      ObjectMapper objectMapper,
      Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter)
      throws Exception {
    ErrorDtoAuthenticationEntryPoint entryPoint =
        new ErrorDtoAuthenticationEntryPoint(objectMapper);
    ErrorDtoAccessDeniedHandler denied = new ErrorDtoAccessDeniedHandler(objectMapper);
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/error",
                        "/actuator/health",
                        "/actuator/info",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/rooms/statistics")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/rooms/*/confirm-availability",
                        "/api/rooms/*/release")
                    .hasRole("SERVICE")
                    .requestMatchers(HttpMethod.POST, "/api/hotels", "/api/hotels/**", "/api/rooms")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/**")
                    .hasAnyRole("USER", "ADMIN")
                    .anyRequest()
                    .hasAnyRole("USER", "ADMIN", "SERVICE"))
        .exceptionHandling(
            errors -> errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
        .oauth2ResourceServer(
            oauth ->
                oauth
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(denied)
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
    return http.build();
  }
}
