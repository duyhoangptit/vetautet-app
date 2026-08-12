package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.messaging.model.KafkaMessageProcessStatus;
import com.vetautet.app.domain.messaging.model.ProcessedKafkaMessage;
import com.vetautet.app.domain.messaging.repository.ProcessedKafkaMessageRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.ProcessedKafkaMessageJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.ProcessedKafkaMessageEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class ProcessedKafkaMessageRepositoryAdapter implements ProcessedKafkaMessageRepository {

    private final ProcessedKafkaMessageJpaRepository jpaRepository;
    private final ProcessedKafkaMessageEntityMapper mapper;

    @Override
    public ProcessedKafkaMessage save(ProcessedKafkaMessage processedKafkaMessage) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(processedKafkaMessage)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedKafkaMessage> findById(UUID processedMessageId) {
        return jpaRepository.findById(processedMessageId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedKafkaMessage> findByOffset(String consumerGroup, String topicName, Integer partitionNo, Long messageOffset) {
        return jpaRepository.findByConsumerGroupAndTopicNameAndPartitionNoAndMessageOffset(consumerGroup, topicName, partitionNo, messageOffset)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessedKafkaMessage> findByTopicAndEventKey(String topicName, String eventKey) {
        return jpaRepository.findByTopicNameAndEventKey(topicName, eventKey).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessedKafkaMessage> findByStatus(KafkaMessageProcessStatus processStatus) {
        return jpaRepository.findByProcessStatus(processStatus).stream().map(mapper::toDomain).toList();
    }
}