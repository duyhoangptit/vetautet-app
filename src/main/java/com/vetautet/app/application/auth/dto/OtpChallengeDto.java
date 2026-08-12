package com.vetautet.app.application.auth.dto;

import com.vetautet.app.domain.auth.model.AuthFlowType;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

/**
 * Shared OTP challenge response payload.
 */
@Data
@Builder
public class OtpChallengeDto {

    private UUID otpSessionId;
    private AuthFlowType flowType;
    private String destination;
    private Instant expiresAt;
    private String referenceToken;

}