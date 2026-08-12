package com.vetautet.app.domain.user.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing an Email address
 * Immutable and validates email format
 */
public final class Email {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final String value;

    private Email(String value) {
        Objects.requireNonNull(value, "Email cannot be null");
        String trimmedEmail = value.trim().toLowerCase();

        if (trimmedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        if (trimmedEmail.length() > 100) {
            throw new IllegalArgumentException("Email cannot be longer than 100 characters");
        }

        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }

        this.value = trimmedEmail;
    }

    public static Email of(String value) {
        return new Email(value);
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
        Email email = (Email) o;
        return Objects.equals(value, email.value);
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
