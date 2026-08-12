package com.vetautet.app.application.messaging.port.input;

import com.vetautet.app.application.messaging.dto.DispatchOutboxCommand;
import com.vetautet.app.application.messaging.dto.DispatchOutboxResultDto;

public interface DispatchOutboxUseCase {

    DispatchOutboxResultDto execute(DispatchOutboxCommand command);
}
