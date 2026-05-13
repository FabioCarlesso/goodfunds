package com.goodfunds.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMillis
) {
    public static AuthResponse bearer(String token, long expiresInMillis) {
        return new AuthResponse(token, "Bearer", expiresInMillis);
    }
}
