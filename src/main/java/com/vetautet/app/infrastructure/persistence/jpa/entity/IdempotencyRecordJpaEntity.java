
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.infrastructure.idempotency.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
* Persistence model for idempotent command execution.
*/
@Entity
@Table(name = "idempotency_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idempotency_operation_key", columnNames = {"operation",
                "idempotency_key"})
}, indexes = {
        @Index(name = "idx_idempotency_status", columnList = "status"),
        @Index(name = "idx_idempotency_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecordJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "idempotency_record_id", nullable = false, updatable = false)
    private UUID idempotencyRecordId;

    @Column(name = "operation", nullable = false, length = 150)
    private String operation;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}