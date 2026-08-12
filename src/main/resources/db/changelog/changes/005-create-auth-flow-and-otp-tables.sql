--liquibase formatted sql

--changeset vetautet-dev:005-create-auth-flow-and-otp-tables
--comment Create auth_flow_tokens and otp_sessions tables for link-based auth flows and reusable OTP handling
CREATE TABLE auth_flow_tokens
(
    auth_flow_token_id UUID         NOT NULL,
    user_id            UUID,
    email              VARCHAR(100) NOT NULL,
    flow_type          VARCHAR(30)  NOT NULL,
    token              VARCHAR(100) NOT NULL,
    expires_at         TIMESTAMP    NOT NULL,
    consumed_at        TIMESTAMP,
    status             VARCHAR(20)  NOT NULL,
    created_date       TIMESTAMP    NOT NULL,
    last_modified_date TIMESTAMP,
    created_by         VARCHAR(50),
    last_modified_by   VARCHAR(50),
    version            BIGINT,
    CONSTRAINT pk_auth_flow_tokens PRIMARY KEY (auth_flow_token_id),
    CONSTRAINT uk_auth_flow_tokens_token UNIQUE (token),
    CONSTRAINT fk_auth_flow_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE INDEX idx_auth_flow_tokens_email_flow ON auth_flow_tokens (email, flow_type, status);

CREATE TABLE otp_sessions
(
    otp_session_id        UUID         NOT NULL,
    user_id               UUID,
    email                 VARCHAR(100) NOT NULL,
    flow_type             VARCHAR(30)  NOT NULL,
    reference_token       VARCHAR(100),
    otp_code              VARCHAR(6)   NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    attempt_count         INT          NOT NULL,
    max_attempts          INT          NOT NULL,
    expires_at            TIMESTAMP    NOT NULL,
    verified_at           TIMESTAMP,
    pending_password_hash VARCHAR(255),
    created_date          TIMESTAMP    NOT NULL,
    last_modified_date    TIMESTAMP,
    created_by            VARCHAR(50),
    last_modified_by      VARCHAR(50),
    version               BIGINT,
    CONSTRAINT pk_otp_sessions PRIMARY KEY (otp_session_id),
    CONSTRAINT fk_otp_sessions_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE INDEX idx_otp_sessions_email_flow ON otp_sessions (email, flow_type, status);
CREATE INDEX idx_otp_sessions_reference ON otp_sessions (reference_token);
CREATE INDEX idx_otp_sessions_expires_at ON otp_sessions (expires_at);

--rollback DROP INDEX IF EXISTS idx_otp_sessions_expires_at;
--rollback DROP INDEX IF EXISTS idx_otp_sessions_reference;
--rollback DROP INDEX IF EXISTS idx_otp_sessions_email_flow;
--rollback DROP TABLE IF EXISTS otp_sessions CASCADE;
--rollback DROP INDEX IF EXISTS idx_auth_flow_tokens_email_flow;
--rollback DROP TABLE IF EXISTS auth_flow_tokens CASCADE;
