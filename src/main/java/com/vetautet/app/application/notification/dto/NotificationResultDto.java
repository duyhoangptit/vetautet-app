package com.vetautet.app.application.notification.dto;

import com.vetautet.app.domain.notification.model.NotificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class NotificationResultDto {

    private final UUID notificationLogId;
    private final NotificationStatus status;
}




 