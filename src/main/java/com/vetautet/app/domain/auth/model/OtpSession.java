package com.vetautet.app.domain.auth.model;

import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.domain.user.model.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
* Shared OTP session model used by authentication flows.
*/
@Getter
@Builder(toBuilder = true)
public class OtpSession {

    private final UUID otpSessionId;
    private final UserId userId;
    private final Email email;
    private final AuthFlowType flowType;
    private final String referenceToken;
    private final String otpCode;
    private final OtpSessionStatus status;
    private final Integer attemptCount;
    private final Integer maxAttempts;
    private final Instant expiresAt;
    private final Instant verifiedAt;
    private final String data;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;

    public boolean isActiveAt(Instant currentTime) {
        return status == OtpSessionStatus.ACTIVE && expiresAt != null && expiresAt.isAfter(currentTime);
    }

    public boolean hasExceededAttempts() {
        return attemptCount != null && maxAttempts != null && attemptCount >= maxAttempts;
    }

    public OtpSession incrementAttempt() {
        return toBuilder()
                .attemptCount((attemptCount == null ? 0 : attemptCount) + 1)
                .build();
    }

    public OtpSession verify(Instant currentTime) {
        return toBuilder()
                .status(OtpSessionStatus.VERIFIED)
                .verifiedAt(currentTime)
                .build();
    }

    public OtpSession deactivate() {
        return toBuilder()
                .status(OtpSessionStatus.INACTIVE)
                .build();
    }

    public OtpSession expire() {
        return toBuilder()
                .status(OtpSessionStatus.EXPIRED)
                .build();
    }
}