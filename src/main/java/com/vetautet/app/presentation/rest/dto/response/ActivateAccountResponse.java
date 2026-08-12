package com.vetautet.app.presentation.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivateAccountResponse {
    private boolean activated;
    private UUID userId;
    private OtpChallengeResponse otpChallenge;
}
 