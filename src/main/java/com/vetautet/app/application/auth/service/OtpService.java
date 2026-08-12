package com.vetautet.app.application.auth.service;

import com.vetautet.app.application.auth.dto.OtpChallengeDto;
import com.vetautet.app.application.auth.port.output.AuthNotificationSender;
import com.vetautet.app.application.auth.port.output.OtpCodeGenerator;
import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.domain.auth.model.OtpSession;
import com.vetautet.app.domain.auth.model.OtpSessionStatus;
import com.vetautet.app.domain.auth.repository.OtpSessionRepository;
import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.domain.user.model.User;
import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
* Shared OTP service for send, resend and verify operations.
*/
@Service
@RequiredArgsConstructor
@Transactional
public class OtpService {

    private final OtpSessionRepository otpSessionRepository;
    private final OtpCodeGenerator otpCodeGenerator;
    private final AuthNotificationSender notificationSender;

    @Value("${app.auth.otp-expiration-minutes:5}")
    private long otpExpirationMinutes;

    @Value("${app.auth.otp-max-attempts:5}")
    private int otpMaxAttempts;

    public OtpChallengeDto sendOtp(User user, AuthFlowType flowType, String referenceToken, String data) {
        return sendOtp(user.getUserId() != null ? user.getUserId().getValue() : null,
                user.getEmail().getValue(),
                flowType,
                referenceToken,
                data);
    }

    public OtpChallengeDto sendOtp(UUID userId, String emailAddress, AuthFlowType flowType, String referenceToken,
                                   String data) {
        Email email = Email.of(emailAddress);
        otpSessionRepository.deactivateActiveSessions(email, flowType, referenceToken);

        String otpCode = otpCodeGenerator.generateSixDigitCode();
        Instant expiresAt = Instant.now().plusSeconds(otpExpirationMinutes * 60);

        OtpSession otpSession = OtpSession.builder()
                .otpSessionId(UUID.randomUUID())
                .userId(userId != null ? com.vetautet.app.domain.user.model.UserId.of(userId) : null)
                .email(email)
                .flowType(flowType)
                .referenceToken(referenceToken)
                .otpCode(otpCode)
                .status(OtpSessionStatus.ACTIVE)
                .attemptCount(0)
                .maxAttempts(otpMaxAttempts)
                .expiresAt(expiresAt)
                .data(data)
                .build();

        OtpSession savedSession = otpSessionRepository.save(otpSession);
        notificationSender.sendOtp(email.getValue(), flowType, otpCode);

        return OtpChallengeDto.builder()
                .otpSessionId(savedSession.getOtpSessionId())
                .flowType(flowType)
                .destination(email.getValue())
                .expiresAt(savedSession.getExpiresAt())
                .referenceToken(referenceToken)
                .build();
    }

    public OtpChallengeDto resendOtp(String emailAddress, AuthFlowType flowType, String referenceToken,
                                     String pendingPasswordHash) {
        return sendOtp(null, emailAddress, flowType, referenceToken, pendingPasswordHash);
    }

    public OtpSession verifyOtp(UUID otpSessionId, String otpCode, AuthFlowType expectedFlowType) {
        OtpSession otpSession = otpSessionRepository.findById(otpSessionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.OTP_SESSION_NOT_FOUND, otpSessionId));

        if (otpSession.getFlowType() != expectedFlowType) {
            throw new AppLogicException(ErrorCode.INVALID_OTP, otpSessionId);
        }

        Instant currentTime = Instant.now();
        if (otpSession.getStatus() != OtpSessionStatus.ACTIVE) {
            throw new AppLogicException(ErrorCode.INVALID_OTP, otpSessionId);
        }

        if (!otpSession.isActiveAt(currentTime)) {
            otpSessionRepository.save(otpSession.expire());
            throw new AppLogicException(ErrorCode.OTP_EXPIRED, otpSessionId);
        }

        if (otpSession.hasExceededAttempts()) {
            throw new AppLogicException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED, otpSessionId);
        }

        if (!otpSession.getOtpCode().equals(otpCode)) {
            OtpSession attemptedSession = otpSession.incrementAttempt();
            if (attemptedSession.hasExceededAttempts()) {
                attemptedSession = attemptedSession.expire();
            }
            otpSessionRepository.save(attemptedSession);
            throw new AppLogicException(ErrorCode.INVALID_OTP, otpSessionId);
        }

        OtpSession verifiedSession = otpSession.verify(currentTime);
        return otpSessionRepository.save(verifiedSession);
    }
}