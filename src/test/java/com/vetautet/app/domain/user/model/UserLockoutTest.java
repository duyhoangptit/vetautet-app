package com.vetautet.app.domain.user.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the login lockout business logic on {@link User}.
 */
class UserLockoutTest {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    private User newUser() {
        return User.builder()
                .userId(UserId.of(java.util.UUID.randomUUID()))
                .personalInfo(PersonalInfo.builder().firstName("Test").lastName("User").build())
                .email(Email.of("test@example.com"))
                .status(UserStatus.ACTIVE)
                .isOnboarding(false)
                .passwordHash("hash")
                .build();
    }

    @Test
    void recordFailedLogin_belowThreshold_incrementsButDoesNotLock() {
        User user = newUser();
        Instant now = Instant.now();

        User afterOneFailure = user.recordFailedLogin(MAX_ATTEMPTS, LOCK_DURATION, now);

        assertThat(afterOneFailure.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(afterOneFailure.getLockedUntil()).isNull();
        assertThat(afterOneFailure.isLocked(now)).isFalse();
    }

    @Test
    void recordFailedLogin_reachingThreshold_locksAccount() {
        User user = newUser();
        Instant now = Instant.now();

        User afterFailures = user;
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            afterFailures = afterFailures.recordFailedLogin(MAX_ATTEMPTS, LOCK_DURATION, now);
        }
        assertThat(afterFailures.isLocked(now)).isFalse();

        User lockedUser = afterFailures.recordFailedLogin(MAX_ATTEMPTS, LOCK_DURATION, now);

        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(lockedUser.getLockedUntil()).isEqualTo(now.plus(LOCK_DURATION));
        assertThat(lockedUser.isLocked(now)).isTrue();
    }

    @Test
    void isLocked_returnsFalse_onceLockedUntilHasPassed() {
        User user = newUser().toBuilder()
                .failedLoginAttempts(MAX_ATTEMPTS)
                .lockedUntil(Instant.now().minusSeconds(1))
                .build();

        assertThat(user.isLocked(Instant.now())).isFalse();
    }

    @Test
    void isLocked_returnsFalse_whenNeverLocked() {
        User user = newUser();

        assertThat(user.isLocked(Instant.now())).isFalse();
    }

    @Test
    void recordSuccessfulLogin_clearsAttemptsAndLock() {
        User lockedUser = newUser().toBuilder()
                .failedLoginAttempts(MAX_ATTEMPTS)
                .lockedUntil(Instant.now().plus(LOCK_DURATION))
                .build();

        User afterSuccess = lockedUser.recordSuccessfulLogin();

        assertThat(afterSuccess.getFailedLoginAttempts()).isZero();
        assertThat(afterSuccess.getLockedUntil()).isNull();
    }

    @Test
    void unlockLogin_clearsAttemptsAndLock() {
        User lockedUser = newUser().toBuilder()
                .failedLoginAttempts(MAX_ATTEMPTS)
                .lockedUntil(Instant.now().plus(LOCK_DURATION))
                .build();

        User unlocked = lockedUser.unlockLogin();

        assertThat(unlocked.getFailedLoginAttempts()).isZero();
        assertThat(unlocked.getLockedUntil()).isNull();
        assertThat(unlocked.isLocked(Instant.now())).isFalse();
    }
}
