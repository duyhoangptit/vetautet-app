package com.vetautet.app.application.auth.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;


@Data
@Builder
public class ResetPasswordCommand {
    private String resetToken;
    private String newPassword;
    private UUID otpSessionId;
    private String otpCode;
}

