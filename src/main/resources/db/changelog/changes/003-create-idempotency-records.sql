--liquibase formatted sql

--changeset vetautet-dev:003-create-idempotency-records
--comment Create idempotency_records table for hybrid idempotent command handling
CREATE TABLE idempotency_records
(
    idempotency_record_id UUID         NOT NULL,
    operation             VARCHAR(150) NOT NULL,
    idempotency_key       VARCHAR(255) NOT NULL,
    request_hash          VARCHAR(64)  NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    http_status           INT,
    response_body         TEXT,
    error_code            VARCHAR(50),
    failure_message       VARCHAR(1000),
    expires_at            TIMESTAMP    NOT NULL,
    completed_at          TIMESTAMP,
    created_date          TIMESTAMP    NOT NULL,
    last_modified_date    TIMESTAMP,
    created_by            VARCHAR(50),
    last_modified_by      VARCHAR(50),
    version               BIGINT,
    CONSTRAINT pk_idempotency_records PRIMARY KEY (idempotency_record_id),
    CONSTRAINT uk_idempotency_operation_key UNIQUE (operation, idempotency_key)
);

CREATE INDEX idx_idempotency_status ON idempotency_records (status);
CREATE INDEX idx_idempotency_expires_at ON idempotency_records (expires_at);

--rollback DROP TABLE IF EXISTS idempotency_records CASCADE;
