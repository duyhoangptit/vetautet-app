package com.vetautet.app.application.payment.port.input;

import com.vetautet.app.application.payment.dto.ConfirmPaymentCommand;
import com.vetautet.app.application.payment.dto.ConfirmPaymentResultDto;

public interface ConfirmPaymentUseCase {

    ConfirmPaymentResultDto execute(ConfirmPaymentCommand command);
}