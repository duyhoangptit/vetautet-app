--liquibase formatted sql

--changeset vetautet-dev:004-align-user-and-rsa-schema
--comment Align users and rsa_key_pairs schema with UUID-based JPA mappings and remove member_info
ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_status;

DROP TABLE IF EXISTS member_info CASCADE;

ALTER TABLE users RENAME COLUMN first_name TO pii_first_name;
ALTER TABLE users RENAME COLUMN last_name TO pii_last_name;
ALTER TABLE users RENAME COLUMN created_at TO created_date;
ALTER TABLE users RENAME COLUMN updated_at TO last_modified_date;
ALTER TABLE users RENAME COLUMN updated_by TO last_modified_by;

ALTER TABLE users
    ADD COLUMN user_id UUID;

WITH generated_ids AS (SELECT id,
                              md5(random()::text || clock_timestamp()::text || id::text) AS uuid_hash
                       FROM users
                       WHERE user_id IS NULL)
UPDATE users u
SET user_id = (
    substr(g.uuid_hash, 1, 8) || '-' ||
    substr(g.uuid_hash, 9, 4) || '-' ||
    substr(g.uuid_hash, 13, 4) || '-' ||
    substr(g.uuid_hash, 17, 4) || '-' ||
    substr(g.uuid_hash, 21, 12)
    )::uuid
FROM generated_ids g
WHERE u.id = g.id;

UPDATE users
SET status = CASE status
                 WHEN 'PENDING' THEN 'PND'
                 WHEN 'ACTIVE' THEN 'ACT'
                 WHEN 'INACTIVE' THEN 'INA'
                 WHEN 'SUSPENDED' THEN 'SUS'
                 WHEN 'DELETED' THEN 'DEL'
                 ELSE status
    END;

ALTER TABLE users
    ALTER COLUMN user_id SET NOT NULL,
ALTER
COLUMN username TYPE VARCHAR(100),
    ALTER
COLUMN avatar_url TYPE VARCHAR(100),
    ALTER
COLUMN status TYPE VARCHAR(3),
    ALTER
COLUMN created_by TYPE VARCHAR(50),
    ALTER
COLUMN last_modified_by TYPE VARCHAR(50),
    ALTER
COLUMN last_modified_date DROP
NOT NULL;

ALTER TABLE users
    ADD COLUMN version BIGINT;

ALTER TABLE users DROP CONSTRAINT pk_users;
ALTER TABLE users
    ADD CONSTRAINT pk_users PRIMARY KEY (user_id);
ALTER TABLE users
    ADD CONSTRAINT ck_users_status CHECK (status IN ('PND', 'ACT', 'INA', 'SUS', 'DEL'));
ALTER TABLE users DROP COLUMN id;

DROP TABLE IF EXISTS rsa_key_pairs CASCADE;

CREATE TABLE rsa_key_pairs
(
    key_pair_id        UUID                 NOT NULL,
    key_id             VARCHAR(100)         NOT NULL UNIQUE,
    user_id            UUID                 NOT NULL,
    public_key         TEXT                 NOT NULL,
    algorithm          VARCHAR(50)          NOT NULL,
    key_size           INT                  NOT NULL,
    is_active          BOOLEAN DEFAULT TRUE NOT NULL,
    expires_at         TIMESTAMP,
    created_date       TIMESTAMP            NOT NULL,
    last_modified_date TIMESTAMP,
    created_by         VARCHAR(50),
    last_modified_by   VARCHAR(50),
    version            BIGINT,
    CONSTRAINT pk_rsa_key_pairs PRIMARY KEY (key_pair_id),
    CONSTRAINT fk_rsa_key_pairs_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE INDEX idx_rsa_key_pair_key_id ON rsa_key_pairs (key_id);
CREATE INDEX idx_rsa_key_pair_user_id ON rsa_key_pairs (user_id);
CREATE INDEX idx_rsa_key_pair_is_active ON rsa_key_pairs (is_active);

--rollback DROP INDEX IF EXISTS idx_rsa_key_pair_is_active;
--rollback DROP INDEX IF EXISTS idx_rsa_key_pair_user_id;
--rollback DROP INDEX IF EXISTS idx_rsa_key_pair_key_id;
--rollback DROP TABLE IF EXISTS rsa_key_pairs CASCADE;
