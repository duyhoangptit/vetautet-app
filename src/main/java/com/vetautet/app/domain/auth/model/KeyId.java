package com.vetautet.app.domain.auth.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a Key ID
 * Used to identify RSA key pairs
 */
public final class KeyId {

    private final String value;

    private KeyId(String value) {
        Objects.requireNonNull(value, "Key ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Key ID cannot be empty");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("Key ID cannot exceed 100 characters");
        }
        this.value = value.trim();
    }

    public static KeyId of(String value) {
        return new KeyId(value);
    }

    public static KeyId generate() {
        String generatedId = "kid_" + UUID.randomUUID().toString().replace("-", "");
        return new KeyId(generatedId);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        KeyId keyId = (KeyId) o;
        return Objects.equals(value, keyId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
