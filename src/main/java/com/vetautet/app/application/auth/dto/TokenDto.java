package com.vetautet.app.application.auth.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * DTO for token response
 * Application layer - use case output
 */
@Data
@Builder
public class TokenDto {
    private String accessToken;
    private String tokenType;
    private Instant expiresAt;
    private String keyId;
}