--liquibase formatted sql

--changeset vetautet-dev:002-add-password-hash
--comment Add password_hash column to users table for password-based authentication
ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255);

--rollback ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
