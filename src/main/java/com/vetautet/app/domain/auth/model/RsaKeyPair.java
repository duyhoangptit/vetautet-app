package com.vetautet.app.domain.auth.model;

import com.vetautet.app.domain.user.model.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;

/**
* RsaKeyPair Domain Entity (Aggregate Root)
* Represents an RSA key pair for JWT signing/verification
* Core business model - no framework dependencies
*/
@Getter
@Builder(toBuilder = true)
public class RsaKeyPair {

    private final KeyId keyId;
    private final UserId userId;
    private final PublicKey publicKey;
    private final String algorithm;
    private final Integer keySize;
    private final Boolean isActive;
    private final LocalDateTime expiresAt;

    // Audit fields
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;

    /**
     * Domain validation
     */
    public void validate() {
        if (keyId == null) {
            throw new IllegalArgumentException("Key ID is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key is required");
        }
        if (algorithm == null || algorithm.trim().isEmpty()) {
            throw new IllegalArgumentException("Algorithm is required");
        }
        if (keySize == null || keySize < 2048) {
            throw new IllegalArgumentException("Key size must be at least 2048 bits");
        }
        if (isActive == null) {
            throw new IllegalArgumentException("Active status is required");
        }
    }

    /**
     * Business logic: Check if key is currently valid
     */
    public boolean isValid() {
        if (!isActive) {
            return false;
        }
        if (expiresAt == null) {
            return true;
        }
        return LocalDateTime.now().isBefore(expiresAt);
    }

    /**
     * Business logic: Check if key has expired
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Business logic: Deactivate key
     */
    public RsaKeyPair deactivate() {
        if (!isActive) {
            throw new IllegalStateException("Key is already inactive");
        }
        return this.toBuilder()
                .isActive(false)
                .build();
    }

    /**
     * Business logic: Check if key can be used for signing
     */
    public boolean canSign() {
        return isValid() && !isExpired();
    }
}
 