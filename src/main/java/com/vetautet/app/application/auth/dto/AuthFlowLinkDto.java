package com.vetautet.app.application.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Link token details returned for auth flows that start from an emailed link.
 */
@Data
@Builder
public class AuthFlowLinkDto {
    private String token;
    private String link;
    private Instant expiresAt;
}