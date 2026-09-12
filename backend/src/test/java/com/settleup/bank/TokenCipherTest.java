package com.settleup.bank;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void encryptsAndDecryptsAccessToken() {
        TokenCipher cipher = new TokenCipher(KEY, new FixedSecureRandom());

        String encrypted = cipher.encrypt("access-sandbox-token");

        assertThat(encrypted).startsWith("v1:").doesNotContain("access-sandbox-token");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("access-sandbox-token");
    }

    @Test
    void rejectsInvalidKeyLength() {
        String shortKey = Base64.getEncoder().encodeToString("too-short".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> new TokenCipher(shortKey, new FixedSecureRandom()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }

    private static final class FixedSecureRandom extends SecureRandom {
        @Override
        public void nextBytes(byte[] bytes) {
            for (int index = 0; index < bytes.length; index++) {
                bytes[index] = (byte) index;
            }
        }
    }
}
