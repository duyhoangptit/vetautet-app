--liquibase formatted sql

--changeset vetautet:015-add-login-lockout-to-users
--comment: Add failed_login_attempts and locked_until columns to users for account lockout
ALTER TABLE users
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMP;

--rollback ALTER TABLE users DROP COLUMN IF EXISTS failed_login_attempts, DROP COLUMN IF EXISTS locked_until;
