package com.hotelbooking.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.hotelbooking.common.web.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class StructuredLogTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtProperties jwtProperties;

  @Test
  void errorLogContainsTraceId(CapturedOutput output) throws Exception {
    mockMvc.perform(
        get("/api/hotels/999")
            .header("Authorization", "Bearer " + token())
            .header("traceId", "log-trace"));

    assertThat(output).contains("log-trace");
    assertThat(output).contains("\"traceId\":\"log-trace\"");
  }

  private String token() {
    SecretKey key =
        new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer())
            .subject("log-test")
            .issuedAt(now)
            .expiresAt(now.plus(jwtProperties.ttl()))
            .claim("role", "USER")
            .build();
    return new NimbusJwtEncoder(new ImmutableSecret<>(key))
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
