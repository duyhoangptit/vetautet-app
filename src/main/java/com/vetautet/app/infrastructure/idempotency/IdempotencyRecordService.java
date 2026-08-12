package com.vetautet.app.infrastructure.idempotency;

import com.vetautet.app.infrastructure.persistence.jpa.entity.IdempotencyRecordJpaEntity;
import com.vetautet.app.infrastructure.persistence.jpa.repository.IdempotencyRecordJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
* Persists idempotency state transitions independently from business
* transactions.
*/
@Service
@RequiredArgsConstructor
public class IdempotencyRecordService {

    private static final int MAX_FAILURE_MESSAGE_LENGTH = 1000;

    private final IdempotencyRecordJpaRepository repository;

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecordJpaEntity> findByOperationAndKey(String operation, String idempotencyKey) {
        return repository.findByOperationAndIdempotencyKey(operation, idempotencyKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyRecordJpaEntity createProcessing(String operation, String idempotencyKey,
                                                       String requestHash, Instant expiresAt) {
        IdempotencyRecordJpaEntity entity = IdempotencyRecordJpaEntity.builder()
                .idempotencyRecordId(UUID.randomUUID())
                .operation(operation)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(IdempotencyStatus.PROCESSING)
                .expiresAt(expiresAt)
                .build();

        return repository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyRecordJpaEntity reopenProcessing(UUID recordId, String requestHash, Instant expiresAt) {
        IdempotencyRecordJpaEntity entity = repository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Idempotency record not found: " + recordId));

        entity.setRequestHash(requestHash);
        entity.setStatus(IdempotencyStatus.PROCESSING);
        entity.setExpiresAt(expiresAt);
        entity.setHttpStatus(null);
        entity.setResponseBody(null);
        entity.setErrorCode(null);
        entity.setFailureMessage(null);
        entity.setCompletedAt(null);

        return repository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(UUID recordId, int httpStatus, String responseBody, Instant completedAt) {
        IdempotencyRecordJpaEntity entity = repository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Idempotency record not found: " + recordId));

        entity.setStatus(IdempotencyStatus.SUCCESS);
        entity.setHttpStatus(httpStatus);
        entity.setResponseBody(responseBody);
        entity.setErrorCode(null);
        entity.setFailureMessage(null);
        entity.setCompletedAt(completedAt);
        repository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID recordId, String errorCode, String failureMessage) {
        IdempotencyRecordJpaEntity entity = repository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Idempotency record not found: " + recordId));

        entity.setStatus(IdempotencyStatus.FAILED);
        entity.setErrorCode(errorCode);
        entity.setFailureMessage(truncate(failureMessage));
        entity.setCompletedAt(Instant.now());
        repository.save(entity);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_FAILURE_MESSAGE_LENGTH) {
            return value;
        }

        return value.substring(0, MAX_FAILURE_MESSAGE_LENGTH);
    }
}