--liquibase formatted sql

--changeset vetautet:011-outbox-add-dlq-status
--comment: Extend outbox_events CHECK constraint to allow DLQ (dead-letter) status

ALTER TABLE outbox_events DROP CONSTRAINT ck_outbox_events_status;

ALTER TABLE outbox_events
    ADD CONSTRAINT ck_outbox_events_status
        CHECK (publish_status IN ('NEW', 'PUB', 'FAI', 'DLQ'));

--rollback ALTER TABLE outbox_events DROP CONSTRAINT ck_outbox_events_status;
--rollback ALTER TABLE outbox_events ADD CONSTRAINT ck_outbox_events_status CHECK (publish_status IN ('NEW', 'PUB', 'FAI'));
