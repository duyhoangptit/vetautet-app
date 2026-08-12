
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.messaging.model.OutboxEvent;
import com.vetautet.app.infrastructure.persistence.jpa.entity.OutboxEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventEntityMapper {

    public OutboxEvent toDomain(OutboxEventJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return OutboxEvent.builder()
                .outboxEventId(entity.getOutboxEventId())
                .aggregateType(entity.getAggregateType())
                .aggregateId(entity.getAggregateId())
                .eventType(entity.getEventType())
                .partitionKey(entity.getPartitionKey())
                .payload(entity.getPayload())
                .headers(entity.getHeaders())
                .publishStatus(entity.getPublishStatus())
                .retryCount(entity.getRetryCount())
                .nextAttemptAt(entity.getNextAttemptAt())
                .publishedAt(entity.getPublishedAt())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public OutboxEventJpaEntity toEntity(OutboxEvent domain) {
        if (domain == null) {
            return null;
        }

        OutboxEventJpaEntity entity = OutboxEventJpaEntity.builder()
                .outboxEventId(domain.getOutboxEventId())
                .aggregateType(domain.getAggregateType())
                .aggregateId(domain.getAggregateId())
                .eventType(domain.getEventType())
                .partitionKey(domain.getPartitionKey())
                .payload(domain.getPayload())
                .headers(domain.getHeaders())
                .publishStatus(domain.getPublishStatus())
                .retryCount(domain.getRetryCount())
                .nextAttemptAt(domain.getNextAttemptAt())
                .publishedAt(domain.getPublishedAt())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }
}