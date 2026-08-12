package com.vetautet.app.domain.messaging.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class OutboxEvent {

    private final UUID outboxEventId;
    private final String aggregateType;
    private final UUID aggregateId;
    private final String eventType;
    private final String partitionKey;
    private final String payload;
    private final String headers;
    private final OutboxPublishStatus publishStatus;
    private final Integer retryCount;
    private final Instant nextAttemptAt;
    private final Instant publishedAt;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;
}