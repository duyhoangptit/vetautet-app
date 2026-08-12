package com.vetautet.app.application.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResetPasswordResultDto {
    private boolean passwordChanged;
    private OtpChallengeDto otpChallenge;
}
