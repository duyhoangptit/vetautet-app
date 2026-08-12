package com.vetautet.app.application.auth.port.input;

import com.vetautet.app.application.auth.dto.ResetPasswordCommand;
import com.vetautet.app.application.auth.dto.ResetPasswordResultDto;

public interface ResetPasswordUseCase {
    ResetPasswordResultDto execute(ResetPasswordCommand command);
}
