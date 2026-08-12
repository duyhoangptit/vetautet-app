package com.vetautet.app.domain.messaging.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vetautet.app.domain.messaging.model.OutboxEvent;
import com.vetautet.app.domain.messaging.model.OutboxPublishStatus;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent outboxEvent);

    Optional<OutboxEvent> findById(UUID outboxEventId);

    List<OutboxEvent> findPendingDispatch(OutboxPublishStatus publishStatus, Instant nextAttemptAt);

    List<OutboxEvent> findDispatchable(List<OutboxPublishStatus> statuses, Instant before);

    List<OutboxEvent> findByAggregate(String aggregateType, UUID aggregateId);
}