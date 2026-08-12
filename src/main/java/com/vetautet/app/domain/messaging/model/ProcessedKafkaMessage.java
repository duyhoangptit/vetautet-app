package com.vetautet.app.domain.messaging.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class ProcessedKafkaMessage {

    private final UUID processedMessageId;
    private final String consumerGroup;
    private final String topicName;
    private final Integer partitionNo;
    private final Long messageOffset;
    private final String eventKey;
    private final KafkaMessageProcessStatus processStatus;
    private final Instant processedAt;
    private final String errorMessage;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;
}