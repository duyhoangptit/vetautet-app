package com.vetautet.app.application.auth.port.input;

import com.vetautet.app.application.auth.dto.AuthFlowLinkDto;
import com.vetautet.app.application.auth.dto.ForgotPasswordCommand;

public interface ForgotPasswordUseCase {
    AuthFlowLinkDto execute(ForgotPasswordCommand command);
}