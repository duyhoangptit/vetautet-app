package com.vetautet.app.application.auth.port.input;

import com.vetautet.app.application.auth.dto.RegisterCommand;
import com.vetautet.app.application.auth.dto.RegisterResultDto;

/**
 * Input port for user self-registration.
 * Application layer - use case interface.
 */
public interface RegisterUseCase {
    RegisterResultDto execute(RegisterCommand command);
}