package com.vetautet.app.application.messaging.dto;

import com.vetautet.app.domain.messaging.model.OutboxPublishStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DispatchedOutboxEventDto {
    private UUID outboxEventId;
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    private OutboxPublishStatus publishStatus;
    private Instant publishedAt;
    private Integer retryCount;
    private String errorMessage;
}

