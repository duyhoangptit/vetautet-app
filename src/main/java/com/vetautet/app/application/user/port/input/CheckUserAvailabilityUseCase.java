package com.vetautet.app.application.user.port.input;

import com.vetautet.app.application.user.dto.AvailabilityCheckType;
import com.vetautet.app.application.user.dto.AvailabilityResult;

/**
 * Input port for checking whether a username or email is already taken.
 * Application layer - use case interface.
 */
public interface CheckUserAvailabilityUseCase {

    AvailabilityResult execute(AvailabilityCheckType type, String value);
}
