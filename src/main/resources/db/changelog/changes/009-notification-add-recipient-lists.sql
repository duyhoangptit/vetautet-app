-- Liquibase formatted SQL

-- ============================================================
-- changeset vetautet:009-notification-recipient-lists
-- comment: Add to_list, cc_list, bcc_list to templates and logs.
--          Template holds static/default addresses; at send time the
--          application merges (appends) request-supplied addresses with
--          template addresses before dispatching. notification_logs stores
--          the final merged lists as a snapshot.
-- ============================================================

-- notification_templates: static recipient lists defined at template level
--   to_list  – static TO addresses always included (e.g. primary distribution group)
--   cc_list  – static CC addresses always included (e.g. team-inbox)
--   bcc_list – static BCC addresses always included (e.g. compliance/audit mailbox)
-- All three are TEXT[] and nullable (NULL = no static addresses for that field).
-- Merge rule: final_list = ARRAY(template_list) || ARRAY(request_list)  (union, dedup by application)
ALTER TABLE notification_templates
    ADD COLUMN to_list  TEXT[] DEFAULT NULL,
    ADD COLUMN cc_list  TEXT[] DEFAULT NULL,
    ADD COLUMN bcc_list TEXT[] DEFAULT NULL;

COMMENT ON COLUMN notification_templates.to_list  IS 'Static TO addresses defined in template. Merged with request to_list at send time.';
COMMENT ON COLUMN notification_templates.cc_list  IS 'Static CC addresses defined in template. Merged with request cc_list at send time.';
COMMENT ON COLUMN notification_templates.bcc_list IS 'Static BCC addresses defined in template. Merged with request bcc_list at send time.';

-- notification_logs: snapshot of the final merged recipient lists after combining
--   template static addresses + request-supplied addresses.
--   to_list  – final TO list used when dispatching
--   cc_list  – final CC list used when dispatching
--   bcc_list – final BCC list used when dispatching
-- The existing `recipient` column remains as the primary single recipient
-- for non-EMAIL channels (SMS, Telegram, Firebase) that do not use to/cc/bcc.
ALTER TABLE notification_logs
    ADD COLUMN to_list  TEXT[] DEFAULT NULL,
    ADD COLUMN cc_list  TEXT[] DEFAULT NULL,
    ADD COLUMN bcc_list TEXT[] DEFAULT NULL;

COMMENT ON COLUMN notification_logs.to_list  IS 'Final merged TO list (template.to_list + request to_list). Snapshot at dispatch time.';
COMMENT ON COLUMN notification_logs.cc_list  IS 'Final merged CC list (template.cc_list + request cc_list). Snapshot at dispatch time.';
COMMENT ON COLUMN notification_logs.bcc_list IS 'Final merged BCC list (template.bcc_list + request bcc_list). Snapshot at dispatch time.';

-- rollback ALTER TABLE notification_logs  DROP COLUMN bcc_list, DROP COLUMN cc_list, DROP COLUMN to_list;
-- rollback ALTER TABLE notification_templates DROP COLUMN bcc_list, DROP COLUMN cc_list, DROP COLUMN to_list;
