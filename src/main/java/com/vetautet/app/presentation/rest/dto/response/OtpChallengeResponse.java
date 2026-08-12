
package com.vetautet.app.presentation.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpChallengeResponse {
    private UUID otpSessionId;
    private String flowType;
    private String maskedEmail;
    private Instant expiresAt;
    private String referenceToken;
}