package com.vetautet.app.presentation.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivateAccountRequest {

    @NotBlank(message = "Activation token is required")
    private String activationToken;

    private UUID otpSessionId;

    private String otpCode;
}











 