package com.vetautet.app.domain.user.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * User Domain Entity (Aggregate Root)
 * Core business model - no framework dependencies
 * Contains business logic and invariants
 */
@Getter
@Builder(toBuilder = true)
public class User {

    private final UserId userId;
    private final String username;
    private final PersonalInfo personalInfo;
    private final Email email;
    private final UserStatus status;
    private final Boolean isOnboarding;
    private final String passwordHash;

    // Login lockout state
    @Builder.Default
    private final int failedLoginAttempts = 0;
    private final Instant lockedUntil;

    // Audit fields
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;

    // role access
    private final List<String> roles;

    /**
     * Domain validation - ensures business rules
     */
    public void validate() {
        if (personalInfo == null) {
            throw new IllegalArgumentException("Personal info is required");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (isOnboarding == null) {
            throw new IllegalArgumentException("Onboarding status is required");
        }
    }

    /**
     * Business logic: Check if user is active
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * Business logic: Check if user can be deleted
     */
    public boolean canBeDeleted() {
        return status == UserStatus.INACTIVE || status == UserStatus.PENDING;
    }

    /**
     * Business logic: Activate user
     */
    public User activate() {
        if (status == UserStatus.ACTIVE) {
            throw new IllegalStateException("User is already active");
        }
        return this.toBuilder()
                .status(UserStatus.ACTIVE)
                .build();
    }

    /**
     * Business logic: Deactivate user
     */
    public User deactivate() {
        if (status == UserStatus.INACTIVE) {
            throw new IllegalStateException("User is already inactive");
        }
        return this.toBuilder()
                .status(UserStatus.INACTIVE)
                .build();
    }

    /**
     * Business logic: Complete onboarding
     */
    public User completeOnboarding() {
        if (!isOnboarding) {
            throw new IllegalStateException("User is not in onboarding state");
        }
        return this.toBuilder()
                .isOnboarding(false)
                .build();
    }

    /**
     * Business logic: Update profile
     */
    public User updateProfile(PersonalInfo newPersonalInfo, Email newEmail) {
        if (newPersonalInfo == null) {
            throw new IllegalArgumentException("Personal info cannot be null");
        }
        if (newEmail == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }

        return this.toBuilder()
                .personalInfo(newPersonalInfo)
                .email(newEmail)
                .build();
    }

    public User updateRoles(List<String> roles) {
        return this.toBuilder()
                .roles(roles)
                .build();
    }

    /**
     * Business logic: Check whether login is currently locked out.
     */
    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /**
     * Business logic: Record a failed login attempt. Locks the account once
     * {@code maxAttempts} consecutive failures are reached.
     */
    public User recordFailedLogin(int maxAttempts, Duration lockDuration, Instant now) {
        int attempts = failedLoginAttempts + 1;
        User.UserBuilder builder = this.toBuilder().failedLoginAttempts(attempts);
        if (attempts >= maxAttempts) {
            builder.lockedUntil(now.plus(lockDuration));
        }
        return builder.build();
    }

    /**
     * Business logic: Clear lockout state after a successful login.
     */
    public User recordSuccessfulLogin() {
        return this.toBuilder()
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build();
    }

    /**
     * Business logic: Clear lockout state, e.g. after a successful password reset.
     */
    public User unlockLogin() {
        return this.toBuilder()
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build();
    }

}
