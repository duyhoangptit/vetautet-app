package com.vetautet.app.domain.auth.service;

import com.vetautet.app.domain.auth.model.RsaKeyPair;
import org.springframework.stereotype.Service;

/**
* Domain service for authentication-related business logic
* Contains logic that doesn't naturally fit in the RsaKeyPair entity
*/
@Service
public class AuthDomainService {

    /**
     * Validate RSA key pair before creation
     */
    public void validateKeyPair(RsaKeyPair keyPair) {
        keyPair.validate();

        // Additional business rules
        if (!keyPair.getAlgorithm().equals("RSA")) {
            throw new IllegalArgumentException("Only RSA algorithm is supported");
        }

        if (keyPair.getKeySize() < 2048) {
            throw new IllegalArgumentException("Key size must be at least 2048 bits for security");
        }

        if (keyPair.getKeySize() > 4096) {
            throw new IllegalArgumentException("Key size cannot exceed 4096 bits");
        }
    }

    /**
     * Check if key can be used for JWT signing
     */
    public boolean canUseForSigning(RsaKeyPair keyPair) {
        return keyPair.canSign();
    }

    /**
     * Validate token expiration time
     */
    public void validateTokenExpiration(int expirationMinutes) {
        if (expirationMinutes < 1) {
            throw new IllegalArgumentException("Token expiration must be at least 1 minute");
        }
        if (expirationMinutes > 1440) { // 24 hours
            throw new IllegalArgumentException("Token expiration cannot exceed 24 hours (1440 minutes)");
        }
    }
}
 