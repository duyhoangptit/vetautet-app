package com.vetautet.app.infrastructure.persistence.jpa.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vetautet.app.domain.messaging.model.OutboxPublishStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.OutboxEventJpaEntity;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    List<OutboxEventJpaEntity> findByPublishStatusAndNextAttemptAtBeforeOrderByCreatedDateAsc(
            OutboxPublishStatus publishStatus,
            Instant nextAttemptAt);

    @Query("SELECT e FROM OutboxEventJpaEntity e " +
           "WHERE e.publishStatus IN :statuses " +
           "AND e.nextAttemptAt <= :before " +
           "ORDER BY e.createdDate ASC")
    List<OutboxEventJpaEntity> findDispatchable(
            @Param("statuses") Collection<OutboxPublishStatus> statuses,
            @Param("before") Instant before);

    List<OutboxEventJpaEntity> findByAggregateTypeAndAggregateIdOrderByCreatedDateAsc(String aggregateType, UUID aggregateId);
}