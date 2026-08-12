package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.messaging.model.ProcessedKafkaMessage;
import com.vetautet.app.infrastructure.persistence.jpa.entity.ProcessedKafkaMessageJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProcessedKafkaMessageEntityMapper {

    public ProcessedKafkaMessage toDomain(ProcessedKafkaMessageJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProcessedKafkaMessage.builder()
                .processedMessageId(entity.getProcessedMessageId())
                .consumerGroup(entity.getConsumerGroup())
                .topicName(entity.getTopicName())
                .partitionNo(entity.getPartitionNo())
                .messageOffset(entity.getMessageOffset())
                .eventKey(entity.getEventKey())
                .processStatus(entity.getProcessStatus())
                .processedAt(entity.getProcessedAt())
                .errorMessage(entity.getErrorMessage())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public ProcessedKafkaMessageJpaEntity toEntity(ProcessedKafkaMessage domain) {
        if (domain == null) {
            return null;
        }

        ProcessedKafkaMessageJpaEntity entity = ProcessedKafkaMessageJpaEntity.builder()
                .processedMessageId(domain.getProcessedMessageId())
                .consumerGroup(domain.getConsumerGroup())
                .topicName(domain.getTopicName())
                .partitionNo(domain.getPartitionNo())
                .messageOffset(domain.getMessageOffset())
                .eventKey(domain.getEventKey())
                .processStatus(domain.getProcessStatus())
                .processedAt(domain.getProcessedAt())
                .errorMessage(domain.getErrorMessage())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }
}