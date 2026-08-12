package com.vetautet.app.application.user.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Result of a best-effort username/email availability check.
 * Application layer - use case DTO.
 */
@Getter
@Builder
public class AvailabilityResult {

    private final AvailabilityCheckType type;
    private final String value;
    private final boolean available;
}
