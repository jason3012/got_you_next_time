package com.settleup.bank;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PlaidWebhookVerifier {

    private static final Duration MAX_AGE = Duration.ofMinutes(5);

    private final PlaidClient plaidClient;
    private final Clock clock;
    private final Map<String, ECKey> keys = new ConcurrentHashMap<>();

    @Autowired
    public PlaidWebhookVerifier(PlaidClient plaidClient) {
        this(plaidClient, Clock.systemUTC());
    }

    PlaidWebhookVerifier(PlaidClient plaidClient, Clock clock) {
        this.plaidClient = plaidClient;
        this.clock = clock;
    }

    public boolean verify(String compactJwt, String rawBody) {
        try {
            SignedJWT jwt = SignedJWT.parse(compactJwt);
            if (!JWSAlgorithm.ES256.equals(jwt.getHeader().getAlgorithm())) {
                return false;
            }
            String keyId = jwt.getHeader().getKeyID();
            if (keyId == null || keyId.isBlank()) {
                return false;
            }
            ECKey key = keys.computeIfAbsent(keyId, this::loadKey);
            if (!jwt.verify(new ECDSAVerifier(key))) {
                return false;
            }
            Instant issuedAt = jwt.getJWTClaimsSet().getIssueTime().toInstant();
            Instant now = clock.instant();
            if (issuedAt.isAfter(now.plusSeconds(30)) || issuedAt.isBefore(now.minus(MAX_AGE))) {
                return false;
            }
            String expectedHash = jwt.getJWTClaimsSet().getStringClaim("request_body_sha256");
            String actualHash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(rawBody.getBytes(StandardCharsets.UTF_8)));
            return expectedHash != null && MessageDigest.isEqual(
                    expectedHash.getBytes(StandardCharsets.US_ASCII),
                    actualHash.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception exception) {
            return false;
        }
    }

    private ECKey loadKey(String keyId) {
        try {
            var keyJson = plaidClient.getWebhookVerificationKey(keyId);
            if (!keyJson.path("expired_at").isNull()
                    && keyJson.path("expired_at").asLong(Long.MAX_VALUE) < clock.instant().getEpochSecond()) {
                throw new IllegalStateException("Plaid webhook verification key is expired");
            }
            ECKey key = ECKey.parse(keyJson.toString());
            if (!keyId.equals(key.getKeyID()) || !JWSAlgorithm.ES256.equals(key.getAlgorithm())) {
                throw new IllegalStateException("Plaid returned a mismatched webhook key");
            }
            return key;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load Plaid webhook verification key", exception);
        }
    }
}
