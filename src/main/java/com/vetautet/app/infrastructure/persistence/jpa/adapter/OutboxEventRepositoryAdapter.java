package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vetautet.app.domain.messaging.model.OutboxEvent;
import com.vetautet.app.domain.messaging.model.OutboxPublishStatus;
import com.vetautet.app.domain.messaging.repository.OutboxEventRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.OutboxEventJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.OutboxEventEntityMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final OutboxEventJpaRepository jpaRepository;
    private final OutboxEventEntityMapper mapper;

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(outboxEvent)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> findById(UUID outboxEventId) {
        return jpaRepository.findById(outboxEventId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPendingDispatch(OutboxPublishStatus publishStatus, Instant nextAttemptAt) {
        return jpaRepository.findByPublishStatusAndNextAttemptAtBeforeOrderByCreatedDateAsc(publishStatus, nextAttemptAt)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findDispatchable(List<OutboxPublishStatus> statuses, Instant before) {
        return jpaRepository.findDispatchable(statuses, before)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findByAggregate(String aggregateType, UUID aggregateId) {
        return jpaRepository.findByAggregateTypeAndAggregateIdOrderByCreatedDateAsc(aggregateType, aggregateId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}