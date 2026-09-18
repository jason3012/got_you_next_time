package com.settleup.auth;

import com.settleup.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import com.settleup.common.exception.UnauthorizedException;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final Duration ttl;
    private final Duration refreshTtl;
    private final JwtDecoder jwtDecoder;

    @Autowired
    public JwtService(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.ttl}") Duration ttl,
            @Value("${security.jwt.refresh-ttl:P30D}") Duration refreshTtl
    ) {
        this(jwtEncoder, jwtDecoder, Clock.systemUTC(), issuer, ttl, refreshTtl);
    }

    JwtService(JwtEncoder jwtEncoder, Clock clock, String issuer, Duration ttl) {
        this(jwtEncoder, null, clock, issuer, ttl, Duration.ofDays(30));
    }

    JwtService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, Clock clock, String issuer, Duration ttl, Duration refreshTtl) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.clock = clock;
        this.issuer = issuer;
        this.ttl = ttl;
        this.refreshTtl = refreshTtl;
    }

    public IssuedToken issue(User user) {
        Instant issuedAt = clock.instant();
        String accessToken = encode(user, issuedAt, ttl, "access");
        String refreshToken = encode(user, issuedAt, refreshTtl, "refresh");
        return new IssuedToken(accessToken, ttl.toSeconds(), refreshToken);
    }

    public UUID refreshSubject(String token) {
        if (jwtDecoder == null) {
            throw new UnauthorizedException("Token refresh is unavailable");
        }
        try {
            var jwt = jwtDecoder.decode(token);
            if (!"refresh".equals(jwt.getClaimAsString("token_type"))) {
                throw new UnauthorizedException("A valid refresh token is required");
            }
            return UUID.fromString(jwt.getSubject());
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new UnauthorizedException("The session has expired");
        }
    }

    private String encode(User user, Instant issuedAt, Duration lifetime, String tokenType) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(lifetime))
                .subject(user.getId().toString())
                .claim("token_type", tokenType)
                .claim("email", user.getEmail())
                .claim("display_name", user.getDisplayName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public record IssuedToken(String value, long expiresInSeconds, String refreshToken) {
        public IssuedToken(String value, long expiresInSeconds) {
            this(value, expiresInSeconds, "test-refresh-token");
        }
    }
}
