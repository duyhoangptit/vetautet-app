package com.vetautet.app.application.auth.port.input;

import com.vetautet.app.application.auth.dto.ActivateAccountCommand;
import com.vetautet.app.application.auth.dto.ActivateAccountResultDto;

public interface ActivateAccountUseCase {
    ActivateAccountResultDto execute(ActivateAccountCommand command);
}