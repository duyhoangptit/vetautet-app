package com.vetautet.app.domain.messaging.repository;

import com.vetautet.app.domain.messaging.model.KafkaMessageProcessStatus;
import com.vetautet.app.domain.messaging.model.ProcessedKafkaMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessedKafkaMessageRepository {

    ProcessedKafkaMessage save(ProcessedKafkaMessage processedKafkaMessage);

    Optional<ProcessedKafkaMessage> findById(UUID processedMessageId);

    Optional<ProcessedKafkaMessage> findByOffset(String consumerGroup, String topicName, Integer partitionNo, Long messageOffset);

    List<ProcessedKafkaMessage> findByTopicAndEventKey(String topicName, String eventKey);

    List<ProcessedKafkaMessage> findByStatus(KafkaMessageProcessStatus processStatus);
}