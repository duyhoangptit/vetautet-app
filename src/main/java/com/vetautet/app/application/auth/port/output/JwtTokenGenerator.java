package com.vetautet.app.application.auth.port.output;

import com.vetautet.app.domain.user.model.User;

import java.security.KeyPair;

/**
 * Output port for JWT token generation
 * This will be implemented by infrastructure layer
 */
public interface JwtTokenGenerator {

    /**
     * Generate JWT token for user
     *
     * @param user              User domain model
     * @param portal            Portal
     * @param keyPair           RSA key pair
     * @param keyId             Key ID
     * @param expirationMinutes Token expiration in minutes
     * @return JWT token string
     */
    String generateToken(User user, String portal, KeyPair keyPair, String keyId, int expirationMinutes);
}