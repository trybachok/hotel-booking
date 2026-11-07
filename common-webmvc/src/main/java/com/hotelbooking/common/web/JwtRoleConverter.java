package com.hotelbooking.common.web;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  static final Set<String> ROLES = Set.of("USER", "ADMIN", "SERVICE");

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String role = jwt.getClaimAsString("role");
    Collection<GrantedAuthority> authorities =
        role == null || !ROLES.contains(role)
            ? List.of()
            : List.of(new SimpleGrantedAuthority("ROLE_" + role));
    return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
  }
}
