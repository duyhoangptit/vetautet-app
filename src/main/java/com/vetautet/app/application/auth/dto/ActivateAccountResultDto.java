package com.vetautet.app.application.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ActivateAccountResultDto {
    private boolean activated;
    private UUID userId;
    private OtpChallengeDto otpChallenge;
}