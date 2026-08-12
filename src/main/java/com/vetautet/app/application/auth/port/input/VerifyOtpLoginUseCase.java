package com.vetautet.app.application.auth.port.input;

import com.vetautet.app.application.auth.dto.TokenDto;
import com.vetautet.app.application.auth.dto.VerifyOtpLoginCommand;

public interface VerifyOtpLoginUseCase {
    TokenDto execute(VerifyOtpLoginCommand command);
}