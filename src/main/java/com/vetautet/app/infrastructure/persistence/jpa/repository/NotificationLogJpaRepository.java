package com.vetautet.app.infrastructure.persistence.jpa.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vetautet.app.infrastructure.persistence.jpa.entity.NotificationLogJpaEntity;

@Repository
public interface NotificationLogJpaRepository extends JpaRepository<NotificationLogJpaEntity, UUID> {

    Optional<NotificationLogJpaEntity> findByEventId(String eventId);

    @Query("""
            SELECT n FROM NotificationLogJpaEntity n
            WHERE n.status = 'FAILED'
              AND n.retryCount < n.maxRetries
              AND n.nextRetryAt <= :now
            ORDER BY n.nextRetryAt ASC
            """)
    List<NotificationLogJpaEntity> findRetryable(@Param("now") Instant now, Pageable pageable);
}
 