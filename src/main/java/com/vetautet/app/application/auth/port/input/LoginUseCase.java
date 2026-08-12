package com.vetautet.app.application.auth.port.input;

import com.vetautet.app.application.auth.dto.LoginCommand;
import com.vetautet.app.application.auth.dto.OtpChallengeDto;

/**
 * Input port for login use case
 * Application layer - use case interface
 */
public interface LoginUseCase {
    OtpChallengeDto execute(LoginCommand command);
}