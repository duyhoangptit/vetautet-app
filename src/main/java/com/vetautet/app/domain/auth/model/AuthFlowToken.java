package com.vetautet.app.domain.auth.model;

import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.domain.user.model.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
* Time-limited link token used to enter an auth flow before OTP verification.
*/
@Getter
@Builder(toBuilder = true)
public class AuthFlowToken {

    private final UUID authFlowTokenId;
    private final UserId userId;
    private final Email email;
    private final AuthFlowType flowType;
    private final String token;
    private final Instant expiresAt;
    private final Instant consumedAt;
    private final AuthFlowTokenStatus status;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;

    public boolean isActiveAt(Instant currentTime) {
        return status == AuthFlowTokenStatus.ACTIVE && expiresAt != null && expiresAt.isAfter(currentTime);
    }

    public AuthFlowToken consume(Instant currentTime) {
        return toBuilder()
                .status(AuthFlowTokenStatus.USED)
                .consumedAt(currentTime)
                .build();
    }

    public AuthFlowToken deactivate() {
        return toBuilder()
                .status(AuthFlowTokenStatus.INACTIVE)
                .build();
    }
}