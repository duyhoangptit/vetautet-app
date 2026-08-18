--liquibase formatted sql

--changeset vetautet-dev:016-add-departure-search-index
--comment: Composite index backing the public departure search query (business_date, status, origin/destination), which now filters origin/destination in SQL instead of in application code
CREATE INDEX idx_train_departures_search ON train_departures (business_date, status, origin_station_id, destination_station_id);

--rollback DROP INDEX IF EXISTS idx_train_departures_search;
