package com.vetautet.app.application.auth.usecase;

import com.vetautet.app.application.auth.dto.LoginCommand;
import com.vetautet.app.application.auth.dto.OtpChallengeDto;
import com.vetautet.app.application.auth.port.input.LoginUseCase;
import com.vetautet.app.application.auth.port.output.PasswordHashEncoder;
import com.vetautet.app.application.auth.service.OtpService;
import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.domain.user.model.User;
import com.vetautet.app.domain.user.repository.UserRepository;
import com.vetautet.app.infrastructure.ratelimit.RateLimited;
import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
* Use case implementation for user login
* Application layer - orchestrates domain logic
*/
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordHashEncoder passwordHashEncoder;
    private final OtpService otpService;

    @Value("${app.auth.login-max-failed-attempts:5}")
    private int loginMaxFailedAttempts;

    @Value("${app.auth.login-lockout-minutes:30}")
    private long loginLockoutMinutes;

    @Override
    @RateLimited(key = "#command.email.toLowerCase()", operation = "auth-login", permits = 5, periodSeconds = 900)
    public OtpChallengeDto execute(LoginCommand command) {
        log.info("Executing LoginUseCase for email: {}", command.getEmail());

        // Find user by email
        Email email = Email.of(command.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.USER_NOT_FOUND_BY_EMAIL, command.getEmail()));

        Instant now = Instant.now();

        // Reject up front if the account is already locked out - avoids a wasted
        // password compare and tells the caller how long is left.
        if (user.isLocked(now)) {
            throw new AppLogicException(ErrorCode.ACCOUNT_LOCKED,
                    loginMaxFailedAttempts, remainingLockMinutes(user, now));
        }

        // Validate password
        if (user.getPasswordHash() == null
                || !passwordHashEncoder.matches(command.getPassword(), user.getPasswordHash())) {
            User afterFailure = user.recordFailedLogin(loginMaxFailedAttempts, Duration.ofMinutes(loginLockoutMinutes), now);
            userRepository.save(afterFailure);

            if (afterFailure.isLocked(now)) {
                log.warn("Account locked after {} failed login attempts for email: {}",
                        loginMaxFailedAttempts, command.getEmail());
                throw new AppLogicException(ErrorCode.ACCOUNT_LOCKED,
                        loginMaxFailedAttempts, remainingLockMinutes(afterFailure, now));
            }
            throw new AppLogicException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Validate user status
        if (!user.isActive()) {
            throw new AppLogicException(ErrorCode.INVALID_USER_STATE, command.getEmail());
        }

        if (user.getFailedLoginAttempts() > 0) {
            userRepository.save(user.recordSuccessfulLogin());
        }

        OtpChallengeDto otpChallenge = otpService.sendOtp(user, AuthFlowType.LOGIN_2FA, null, command.getPortal());
        log.info("OTP challenge {} created for login flow of user {}", otpChallenge.getOtpSessionId(),
                user.getEmail().getValue());
        return otpChallenge;
    }

    private long remainingLockMinutes(User user, Instant now) {
        return Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
    }
}
 