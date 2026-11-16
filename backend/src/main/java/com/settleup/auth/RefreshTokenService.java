package com.settleup.auth;

import com.settleup.common.exception.UnauthorizedException;
import com.settleup.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final Duration refreshTtl;
    private final Clock clock = Clock.systemUTC();

    public RefreshTokenService(
            RefreshTokenRepository repository,
            @Value("${security.jwt.refresh-ttl:P30D}") Duration refreshTtl
    ) {
        this.repository = repository;
        this.refreshTtl = refreshTtl;
    }

    public void store(User user, String rawToken) {
        repository.save(new RefreshToken(user, hash(rawToken), clock.instant().plus(refreshTtl)));
    }

    public void rotate(String rawToken, User expectedUser) {
        RefreshToken token = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("The session is no longer valid"));
        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(clock.instant())
                || !token.getUser().getId().equals(expectedUser.getId())) {
            throw new UnauthorizedException("The session is no longer valid");
        }
        token.revoke(clock.instant());
    }

    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> token.revoke(clock.instant()));
    }

    private String hash(String rawToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash refresh token", exception);
        }
    }
}
