package com.vetautet.app.domain.auth.model;

import java.time.Instant;
import java.util.Objects;

/**
* Value Object representing a JWT Token
* Immutable
*/
public final class Token {

    private final String value;
    private final String type;
    private final Instant expiresAt;

    private Token(String value, String type, Instant expiresAt) {
        Objects.requireNonNull(value, "Token value cannot be null");
        Objects.requireNonNull(type, "Token type cannot be null");
        Objects.requireNonNull(expiresAt, "Token expiration cannot be null");

        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Token value cannot be empty");
        }

        this.value = value.trim();
        this.type = type;
        this.expiresAt = expiresAt;
    }

    public static Token of(String value, String type, Instant expiresAt) {
        return new Token(value, type, expiresAt);
    }

    public static Token bearer(String value, Instant expiresAt) {
        return new Token(value, "Bearer", expiresAt);
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Token token = (Token) o;
        return Objects.equals(value, token.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        // Don't expose full token in toString for security
        return "Token{type='" + type + "', expiresAt=" + expiresAt + "}";
    }
}
 