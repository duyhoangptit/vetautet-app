package com.vetautet.app.application.auth.port.output;

/**
 * Output port for password hashing operations.
 * Application layer - abstracts hashing algorithm from business logic.
 */
public interface PasswordHashEncoder {

    /**
     * Encode a raw password into a hash.
     *
     * @param rawPassword the plain-text password
     * @return the encoded password hash
     */
    String encode(String rawPassword);

    /**
     * Verify that a raw password matches an encoded hash.
     *
     * @param rawPassword     the plain-text password to verify
     * @param encodedPassword the previously encoded hash to compare against
     * @return {@code true} if the password matches, {@code false} otherwise
     */
    boolean matches(String rawPassword, String encodedPassword);
}
