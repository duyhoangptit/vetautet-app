package com.vetautet.app.domain.user.service;

import com.vetautet.app.domain.user.model.User;
import org.springframework.stereotype.Service;

/**
 * Domain service for User
 * Contains business logic that doesn't naturally fit in the User entity
 */
@Service
public class UserDomainService {

    /**
     * Validate a new user before creation
     */
    public void validateNewUser(User user) {
        // Additional domain validations
        user.validate();

        // Example: Business rule - username must not contain special characters if
        // provided
        if (user.getUsername() != null && !user.getUsername().matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores");
        }
    }

    /**
     * Business logic to determine if a user can update another user
     */
    public boolean canUpdate(User updater, User target) {
        // Example business logic
        // In real scenario, this might check roles, permissions, etc.
        return !updater.getUserId().equals(target.getUserId()) || updater.isActive();
    }
}
 