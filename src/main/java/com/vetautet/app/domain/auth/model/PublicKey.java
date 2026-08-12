package com.vetautet.app.domain.auth.model;

import java.util.Objects;

/**
 * Value Object representing RSA Public Key
 * Immutable and validates PEM format
 */
public final class PublicKey {

    private final String pemFormat;

    private PublicKey(String pemFormat) {
        Objects.requireNonNull(pemFormat, "Public key cannot be null");

        String trimmed = pemFormat.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Public key cannot be empty");
        }

        // Basic PEM format validation
        if (!trimmed.startsWith("-----BEGIN") || !trimmed.contains("-----END")) {
            throw new IllegalArgumentException("Invalid PEM format for public key");
        }

        this.pemFormat = trimmed;
    }

    public static PublicKey of(String pemFormat) {
        return new PublicKey(pemFormat);
    }

    public String getPemFormat() {
        return pemFormat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PublicKey publicKey = (PublicKey) o;
        return Objects.equals(pemFormat, publicKey.pemFormat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pemFormat);
    }

    @Override
    public String toString() {
        // Don't expose full key in toString for security
        return "PublicKey{length=" + pemFormat.length() + "}";
    }
}
