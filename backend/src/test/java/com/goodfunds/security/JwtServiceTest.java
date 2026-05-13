package com.goodfunds.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-please-change-this-to-something-long-enough";

    @Test
    void generateToken_isParseableAndReturnsSubject() {
        JwtService service = new JwtService(SECRET, 60_000L);
        String token = service.generateToken("user@example.com");

        assertThat(token).isNotBlank();
        assertThat(service.isValid(token)).isTrue();
        assertThat(service.extractSubject(token)).isEqualTo("user@example.com");
    }

    @Test
    void isValid_returnsFalseForGarbage() {
        JwtService service = new JwtService(SECRET, 60_000L);
        assertThat(service.isValid("not-a-token")).isFalse();
    }

    @Test
    void isValid_returnsFalseWhenSignedByAnotherKey() {
        JwtService alice = new JwtService(SECRET, 60_000L);
        JwtService mallory = new JwtService("different-secret-also-long-enough-to-be-valid", 60_000L);

        String token = mallory.generateToken("user@example.com");
        assertThat(alice.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalseForExpiredToken() throws InterruptedException {
        JwtService service = new JwtService(SECRET, 1L);
        String token = service.generateToken("user@example.com");
        Thread.sleep(50L);
        assertThat(service.isValid(token)).isFalse();
    }

    @Test
    void getExpirationMillis_returnsConfiguredValue() {
        JwtService service = new JwtService(SECRET, 86_400_000L);
        assertThat(service.getExpirationMillis()).isEqualTo(86_400_000L);
    }

    @Test
    void constructor_rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService("short-secret", 60_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
