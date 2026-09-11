package com.settleup.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.settleup.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void issuesSignedTokenWithIdentityAndExpiry() {
        SecretKey key = new SecretKeySpec(
                "a-test-secret-that-is-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtService service = new JwtService(
                new NimbusJwtEncoder(new ImmutableSecret<>(key)),
                Clock.fixed(now, ZoneOffset.UTC),
                "settleup-api",
                Duration.ofHours(1));
        UUID userId = UUID.fromString("5f5f0b31-b1bb-4691-a724-ad43afb6eecb");
        User user = new User("friend@example.com", "Friend", "unused");
        ReflectionTestUtils.setField(user, "id", userId);

        JwtService.IssuedToken issued = service.issue(user);

        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        Jwt jwt = decoder.decode(issued.value());
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("email")).isEqualTo("friend@example.com");
        assertThat(jwt.getClaimAsString("display_name")).isEqualTo("Friend");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plusSeconds(3600));
        assertThat(issued.expiresInSeconds()).isEqualTo(3600);
    }
}
