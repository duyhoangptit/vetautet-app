package com.vetautet.app.application.booking.port.input;

import com.vetautet.app.application.booking.dto.HoldBookingCommand;
import com.vetautet.app.application.booking.dto.HoldBookingResultDto;

public interface HoldBookingUseCase {

    HoldBookingResultDto execute(HoldBookingCommand command);
}