package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.domain.messaging.model.KafkaMessageProcessStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.ProcessedKafkaMessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedKafkaMessageJpaRepository extends JpaRepository<ProcessedKafkaMessageJpaEntity, UUID> {

    Optional<ProcessedKafkaMessageJpaEntity> findByConsumerGroupAndTopicNameAndPartitionNoAndMessageOffset(
            String consumerGroup,
            String topicName,
            Integer partitionNo,
            Long messageOffset);

    List<ProcessedKafkaMessageJpaEntity> findByTopicNameAndEventKey(String topicName, String eventKey);

    List<ProcessedKafkaMessageJpaEntity> findByProcessStatus(KafkaMessageProcessStatus processStatus);
}