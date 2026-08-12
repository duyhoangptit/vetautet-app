package com.vetautet.app.domain.notification.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class NotificationTemplate {

    private final UUID notificationTemplateId;
    private final String templateCode;
    private final NotificationChannelCode channelCode;
    private final String locale;
    private final String templateName;
    private final String subjectTemplate;
    private final String bodyTemplate;
    private final List<String> variableKeys;
    private final List<String> toList;
    private final List<String> ccList;
    private final List<String> bccList;
    private final String description;
    private final Boolean isActive;

    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;
}
 