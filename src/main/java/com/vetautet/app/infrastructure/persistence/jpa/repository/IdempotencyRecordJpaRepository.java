package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.infrastructure.persistence.jpa.entity.IdempotencyRecordJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
* Repository for idempotency records.
*/
@Repository
public interface IdempotencyRecordJpaRepository extends JpaRepository<IdempotencyRecordJpaEntity, UUID> {

    Optional<IdempotencyRecordJpaEntity> findByOperationAndIdempotencyKey(String operation, String idempotencyKey);
}