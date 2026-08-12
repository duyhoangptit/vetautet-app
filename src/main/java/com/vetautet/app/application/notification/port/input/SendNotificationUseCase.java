package com.vetautet.app.application.notification.port.input;

import com.vetautet.app.application.notification.dto.NotificationResultDto;
import com.vetautet.app.application.notification.dto.SendNotificationCommand;

public interface SendNotificationUseCase {

    NotificationResultDto execute(SendNotificationCommand command);
}