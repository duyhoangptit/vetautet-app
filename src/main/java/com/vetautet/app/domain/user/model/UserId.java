package com.vetautet.app.domain.user.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a User ID
 * Immutable and validates the ID format
 */
public final class UserId {

    private final UUID value;

    private UserId(UUID value) {
        Objects.requireNonNull(value, "User ID cannot be null");
        this.value = value;
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String value) {
        try {
            return new UserId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid User ID format: " + value, e);
        }
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserId userId = (UserId) o;
        return Objects.equals(value, userId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
