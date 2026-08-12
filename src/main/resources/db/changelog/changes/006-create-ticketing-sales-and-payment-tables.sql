--liquibase formatted sql

--changeset vetautet-dev:006-create-ticketing-sales-and-payment-tables
--comment Create high-throughput train ticketing, payment and Kafka integration tables
CREATE TABLE railway_stations
(
    station_id          UUID         NOT NULL,
    station_code        VARCHAR(20)  NOT NULL,
    station_name        VARCHAR(150) NOT NULL,
    city_code           VARCHAR(20)  NOT NULL,
    timezone_name       VARCHAR(50)  NOT NULL,
    display_order       INT          NOT NULL DEFAULT 0,
    status              VARCHAR(3)   NOT NULL,
    created_date        TIMESTAMP    NOT NULL,
    last_modified_date  TIMESTAMP,
    created_by          VARCHAR(50),
    last_modified_by    VARCHAR(50),
    version             BIGINT,
    CONSTRAINT pk_railway_stations PRIMARY KEY (station_id),
    CONSTRAINT uk_railway_stations_code UNIQUE (station_code),
    CONSTRAINT ck_railway_stations_status CHECK (status IN ('ACT', 'INA'))
);

CREATE TABLE train_routes
(
    route_id                 UUID         NOT NULL,
    route_code               VARCHAR(30)  NOT NULL,
    route_name               VARCHAR(150) NOT NULL,
    origin_station_id        UUID         NOT NULL,
    destination_station_id   UUID         NOT NULL,
    distance_km              NUMERIC(10, 2),
    status                   VARCHAR(3)   NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    last_modified_date       TIMESTAMP,
    created_by               VARCHAR(50),
    last_modified_by         VARCHAR(50),
    version                  BIGINT,
    CONSTRAINT pk_train_routes PRIMARY KEY (route_id),
    CONSTRAINT uk_train_routes_code UNIQUE (route_code),
    CONSTRAINT fk_train_routes_origin_station FOREIGN KEY (origin_station_id) REFERENCES railway_stations (station_id),
    CONSTRAINT fk_train_routes_destination_station FOREIGN KEY (destination_station_id) REFERENCES railway_stations (station_id),
    CONSTRAINT ck_train_routes_status CHECK (status IN ('DRF', 'ACT', 'INA'))
);

CREATE TABLE train_route_stops
(
    route_stop_id                    UUID      NOT NULL,
    route_id                         UUID      NOT NULL,
    station_id                       UUID      NOT NULL,
    stop_sequence                    INT       NOT NULL,
    distance_from_origin_km          NUMERIC(10, 2),
    planned_arrival_offset_minutes   INT,
    planned_departure_offset_minutes INT,
    created_date                     TIMESTAMP NOT NULL,
    last_modified_date               TIMESTAMP,
    created_by                       VARCHAR(50),
    last_modified_by                 VARCHAR(50),
    version                          BIGINT,
    CONSTRAINT pk_train_route_stops PRIMARY KEY (route_stop_id),
    CONSTRAINT uk_train_route_stop_sequence UNIQUE (route_id, stop_sequence),
    CONSTRAINT uk_train_route_stop_station UNIQUE (route_id, station_id),
    CONSTRAINT fk_train_route_stops_route FOREIGN KEY (route_id) REFERENCES train_routes (route_id),
    CONSTRAINT fk_train_route_stops_station FOREIGN KEY (station_id) REFERENCES railway_stations (station_id)
);

CREATE TABLE trains
(
    train_id            UUID         NOT NULL,
    train_code          VARCHAR(30)  NOT NULL,
    train_name          VARCHAR(150) NOT NULL,
    seat_layout_version VARCHAR(30)  NOT NULL,
    operator_code       VARCHAR(30)  NOT NULL,
    status              VARCHAR(3)   NOT NULL,
    created_date        TIMESTAMP    NOT NULL,
    last_modified_date  TIMESTAMP,
    created_by          VARCHAR(50),
    last_modified_by    VARCHAR(50),
    version             BIGINT,
    CONSTRAINT pk_trains PRIMARY KEY (train_id),
    CONSTRAINT uk_trains_code UNIQUE (train_code),
    CONSTRAINT ck_trains_status CHECK (status IN ('ACT', 'INA', 'MNT'))
);

CREATE TABLE train_departures
(
    departure_id            UUID         NOT NULL,
    route_id                UUID         NOT NULL,
    train_id                UUID         NOT NULL,
    departure_code          VARCHAR(40)  NOT NULL,
    business_date           DATE         NOT NULL,
    origin_station_id       UUID         NOT NULL,
    destination_station_id  UUID         NOT NULL,
    planned_departure_at    TIMESTAMP    NOT NULL,
    planned_arrival_at      TIMESTAMP    NOT NULL,
    sale_opens_at           TIMESTAMP    NOT NULL,
    sale_closes_at          TIMESTAMP    NOT NULL,
    status                  VARCHAR(3)   NOT NULL,
    created_date            TIMESTAMP    NOT NULL,
    last_modified_date      TIMESTAMP,
    created_by              VARCHAR(50),
    last_modified_by        VARCHAR(50),
    version                 BIGINT,
    CONSTRAINT pk_train_departures PRIMARY KEY (departure_id),
    CONSTRAINT uk_train_departures_code UNIQUE (departure_code),
    CONSTRAINT uk_train_departures_business UNIQUE (route_id, train_id, business_date),
    CONSTRAINT fk_train_departures_route FOREIGN KEY (route_id) REFERENCES train_routes (route_id),
    CONSTRAINT fk_train_departures_train FOREIGN KEY (train_id) REFERENCES trains (train_id),
    CONSTRAINT fk_train_departures_origin_station FOREIGN KEY (origin_station_id) REFERENCES railway_stations (station_id),
    CONSTRAINT fk_train_departures_destination_station FOREIGN KEY (destination_station_id) REFERENCES railway_stations (station_id),
    CONSTRAINT ck_train_departures_status CHECK (status IN ('SCH', 'OPN', 'CLS', 'CNL', 'CMP'))
);

CREATE TABLE departure_inventory_buckets
(
    inventory_bucket_id UUID          NOT NULL,
    departure_id        UUID          NOT NULL,
    seat_class_code     VARCHAR(20)   NOT NULL,
    quota_code          VARCHAR(20)   NOT NULL,
    bucket_no           SMALLINT      NOT NULL,
    total_quantity      INT           NOT NULL,
    available_quantity  INT           NOT NULL,
    reserved_quantity   INT           NOT NULL,
    sold_quantity       INT           NOT NULL,
    oversell_limit      INT           NOT NULL DEFAULT 0,
    fare_amount         NUMERIC(18,2) NOT NULL,
    currency_code       CHAR(3)       NOT NULL,
    sale_status         VARCHAR(3)    NOT NULL,
    created_date        TIMESTAMP     NOT NULL,
    last_modified_date  TIMESTAMP,
    created_by          VARCHAR(50),
    last_modified_by    VARCHAR(50),
    version             BIGINT,
    CONSTRAINT pk_departure_inventory_buckets PRIMARY KEY (inventory_bucket_id),
    CONSTRAINT uk_departure_inventory_bucket UNIQUE (departure_id, seat_class_code, quota_code, bucket_no),
    CONSTRAINT fk_departure_inventory_departure FOREIGN KEY (departure_id) REFERENCES train_departures (departure_id),
    CONSTRAINT ck_departure_inventory_quantities CHECK (
        total_quantity >= 0 AND
        available_quantity >= 0 AND
        reserved_quantity >= 0 AND
        sold_quantity >= 0 AND
        oversell_limit >= 0
        ),
    CONSTRAINT ck_departure_inventory_status CHECK (sale_status IN ('OPN', 'HLD', 'CLS'))
);

CREATE TABLE booking_orders
(
    booking_order_id   UUID          NOT NULL,
    order_code         VARCHAR(40)   NOT NULL,
    user_id            UUID,
    departure_id       UUID          NOT NULL,
    booking_channel    VARCHAR(20)   NOT NULL,
    customer_full_name VARCHAR(150)  NOT NULL,
    customer_email     VARCHAR(100)  NOT NULL,
    customer_phone     VARCHAR(30),
    status             VARCHAR(3)    NOT NULL,
    total_amount       NUMERIC(18,2) NOT NULL,
    currency_code      CHAR(3)       NOT NULL,
    hold_expires_at    TIMESTAMP     NOT NULL,
    confirmed_at       TIMESTAMP,
    cancelled_at       TIMESTAMP,
    idempotency_key    VARCHAR(255),
    failure_reason     VARCHAR(500),
    created_date       TIMESTAMP     NOT NULL,
    last_modified_date TIMESTAMP,
    created_by         VARCHAR(50),
    last_modified_by   VARCHAR(50),
    version            BIGINT,
    CONSTRAINT pk_booking_orders PRIMARY KEY (booking_order_id),
    CONSTRAINT uk_booking_orders_code UNIQUE (order_code),
    CONSTRAINT fk_booking_orders_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_booking_orders_departure FOREIGN KEY (departure_id) REFERENCES train_departures (departure_id),
    CONSTRAINT ck_booking_orders_status CHECK (status IN ('HLD', 'PAY', 'CNF', 'EXP', 'CXL', 'FAI', 'RFD'))
);

CREATE TABLE booking_order_items
(
    booking_order_item_id     UUID          NOT NULL,
    booking_order_id          UUID          NOT NULL,
    line_no                   INT           NOT NULL,
    inventory_bucket_id       UUID          NOT NULL,
    travel_from_stop_sequence INT           NOT NULL,
    travel_to_stop_sequence   INT           NOT NULL,
    seat_class_code           VARCHAR(20)   NOT NULL,
    quota_code                VARCHAR(20)   NOT NULL,
    quantity                  INT           NOT NULL,
    unit_price_amount         NUMERIC(18,2) NOT NULL,
    line_total_amount         NUMERIC(18,2) NOT NULL,
    item_status               VARCHAR(3)    NOT NULL,
    created_date              TIMESTAMP     NOT NULL,
    last_modified_date        TIMESTAMP,
    created_by                VARCHAR(50),
    last_modified_by          VARCHAR(50),
    version                   BIGINT,
    CONSTRAINT pk_booking_order_items PRIMARY KEY (booking_order_item_id),
    CONSTRAINT uk_booking_order_item_line UNIQUE (booking_order_id, line_no),
    CONSTRAINT fk_booking_order_items_order FOREIGN KEY (booking_order_id) REFERENCES booking_orders (booking_order_id),
    CONSTRAINT fk_booking_order_items_inventory_bucket FOREIGN KEY (inventory_bucket_id) REFERENCES departure_inventory_buckets (inventory_bucket_id),
    CONSTRAINT ck_booking_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_booking_order_items_status CHECK (item_status IN ('HLD', 'CNF', 'CXL', 'RFD'))
);

CREATE TABLE inventory_reservations
(
    inventory_reservation_id UUID       NOT NULL,
    booking_order_id         UUID       NOT NULL,
    inventory_bucket_id      UUID       NOT NULL,
    reserved_quantity        INT        NOT NULL,
    hold_expires_at          TIMESTAMP  NOT NULL,
    reservation_status       VARCHAR(3) NOT NULL,
    confirmed_at             TIMESTAMP,
    released_at              TIMESTAMP,
    created_date             TIMESTAMP  NOT NULL,
    last_modified_date       TIMESTAMP,
    created_by               VARCHAR(50),
    last_modified_by         VARCHAR(50),
    version                  BIGINT,
    CONSTRAINT pk_inventory_reservations PRIMARY KEY (inventory_reservation_id),
    CONSTRAINT fk_inventory_reservations_order FOREIGN KEY (booking_order_id) REFERENCES booking_orders (booking_order_id),
    CONSTRAINT fk_inventory_reservations_bucket FOREIGN KEY (inventory_bucket_id) REFERENCES departure_inventory_buckets (inventory_bucket_id),
    CONSTRAINT ck_inventory_reservations_quantity CHECK (reserved_quantity > 0),
    CONSTRAINT ck_inventory_reservations_status CHECK (reservation_status IN ('HLD', 'CNF', 'REL', 'EXP'))
);

CREATE TABLE payment_transactions
(
    payment_transaction_id  UUID          NOT NULL,
    booking_order_id        UUID          NOT NULL,
    payment_method_code     VARCHAR(20)   NOT NULL,
    provider_code           VARCHAR(30)   NOT NULL,
    provider_transaction_id VARCHAR(100),
    provider_payment_url    VARCHAR(1000),
    amount                  NUMERIC(18,2) NOT NULL,
    currency_code           CHAR(3)       NOT NULL,
    status                  VARCHAR(3)    NOT NULL,
    requested_at            TIMESTAMP     NOT NULL,
    authorized_at           TIMESTAMP,
    settled_at              TIMESTAMP,
    expires_at              TIMESTAMP,
    failure_code            VARCHAR(50),
    failure_message         VARCHAR(1000),
    created_date            TIMESTAMP     NOT NULL,
    last_modified_date      TIMESTAMP,
    created_by              VARCHAR(50),
    last_modified_by        VARCHAR(50),
    version                 BIGINT,
    CONSTRAINT pk_payment_transactions PRIMARY KEY (payment_transaction_id),
    CONSTRAINT uk_payment_provider_transaction UNIQUE (provider_code, provider_transaction_id),
    CONSTRAINT fk_payment_transactions_order FOREIGN KEY (booking_order_id) REFERENCES booking_orders (booking_order_id),
    CONSTRAINT ck_payment_method_code CHECK (payment_method_code IN ('CC', 'BANK', 'VNPAYQR')),
    CONSTRAINT ck_payment_transaction_status CHECK (status IN ('INI', 'PEN', 'AUT', 'SET', 'FAI', 'CNC', 'REF', 'EXP'))
);

CREATE TABLE payment_provider_callbacks
(
    payment_callback_id     UUID         NOT NULL,
    payment_transaction_id  UUID         NOT NULL,
    provider_code           VARCHAR(30)  NOT NULL,
    provider_event_id       VARCHAR(100) NOT NULL,
    callback_status         VARCHAR(3)   NOT NULL,
    payload                 JSONB        NOT NULL,
    received_at             TIMESTAMP    NOT NULL,
    processed_at            TIMESTAMP,
    error_message           VARCHAR(1000),
    created_date            TIMESTAMP    NOT NULL,
    last_modified_date      TIMESTAMP,
    created_by              VARCHAR(50),
    last_modified_by        VARCHAR(50),
    version                 BIGINT,
    CONSTRAINT pk_payment_provider_callbacks PRIMARY KEY (payment_callback_id),
    CONSTRAINT uk_payment_provider_event UNIQUE (provider_code, provider_event_id),
    CONSTRAINT fk_payment_provider_callbacks_transaction FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions (payment_transaction_id),
    CONSTRAINT ck_payment_callback_status CHECK (callback_status IN ('NEW', 'PRC', 'ERR', 'IGN'))
);

CREATE TABLE outbox_events
(
    outbox_event_id     UUID         NOT NULL,
    aggregate_type      VARCHAR(50)  NOT NULL,
    aggregate_id        UUID         NOT NULL,
    event_type          VARCHAR(100) NOT NULL,
    partition_key       VARCHAR(100) NOT NULL,
    payload             JSONB        NOT NULL,
    headers             JSONB,
    publish_status      VARCHAR(3)   NOT NULL,
    retry_count         INT          NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMP,
    published_at        TIMESTAMP,
    created_date        TIMESTAMP    NOT NULL,
    last_modified_date  TIMESTAMP,
    created_by          VARCHAR(50),
    last_modified_by    VARCHAR(50),
    version             BIGINT,
    CONSTRAINT pk_outbox_events PRIMARY KEY (outbox_event_id),
    CONSTRAINT ck_outbox_events_status CHECK (publish_status IN ('NEW', 'PUB', 'FAI'))
);

CREATE TABLE processed_kafka_messages
(
    processed_message_id UUID         NOT NULL,
    consumer_group       VARCHAR(100) NOT NULL,
    topic_name           VARCHAR(100) NOT NULL,
    partition_no         INT          NOT NULL,
    message_offset       BIGINT       NOT NULL,
    event_key            VARCHAR(255),
    process_status       VARCHAR(3)   NOT NULL,
    processed_at         TIMESTAMP    NOT NULL,
    error_message        VARCHAR(1000),
    created_date         TIMESTAMP    NOT NULL,
    last_modified_date   TIMESTAMP,
    created_by           VARCHAR(50),
    last_modified_by     VARCHAR(50),
    version              BIGINT,
    CONSTRAINT pk_processed_kafka_messages PRIMARY KEY (processed_message_id),
    CONSTRAINT uk_processed_kafka_message UNIQUE (consumer_group, topic_name, partition_no, message_offset),
    CONSTRAINT ck_processed_kafka_messages_status CHECK (process_status IN ('DON', 'ERR'))
);

CREATE INDEX idx_train_departures_business_date ON train_departures (business_date, status);
CREATE INDEX idx_train_departures_route_time ON train_departures (route_id, planned_departure_at);
CREATE INDEX idx_departure_inventory_sale_status ON departure_inventory_buckets (departure_id, sale_status, seat_class_code, quota_code);
CREATE INDEX idx_departure_inventory_hot_bucket ON departure_inventory_buckets (departure_id, seat_class_code, quota_code, bucket_no, available_quantity);
CREATE INDEX idx_booking_orders_user_status ON booking_orders (user_id, status, created_date);
CREATE INDEX idx_booking_orders_departure_status ON booking_orders (departure_id, status, hold_expires_at);
CREATE INDEX idx_booking_orders_hold_expires_at ON booking_orders (hold_expires_at);
CREATE INDEX idx_inventory_reservations_hold_expiry ON inventory_reservations (hold_expires_at, reservation_status);
CREATE INDEX idx_inventory_reservations_order ON inventory_reservations (booking_order_id, reservation_status);
CREATE INDEX idx_payment_transactions_order_status ON payment_transactions (booking_order_id, status, requested_at);
CREATE INDEX idx_payment_transactions_provider_status ON payment_transactions (provider_code, status, expires_at);
CREATE INDEX idx_payment_callbacks_transaction ON payment_provider_callbacks (payment_transaction_id, received_at);
CREATE INDEX idx_outbox_events_dispatch ON outbox_events (publish_status, next_attempt_at, created_date);
CREATE INDEX idx_processed_kafka_messages_key ON processed_kafka_messages (topic_name, event_key);

--rollback DROP INDEX IF EXISTS idx_processed_kafka_messages_key;
--rollback DROP INDEX IF EXISTS idx_outbox_events_dispatch;
--rollback DROP INDEX IF EXISTS idx_payment_callbacks_transaction;
--rollback DROP INDEX IF EXISTS idx_payment_transactions_provider_status;
--rollback DROP INDEX IF EXISTS idx_payment_transactions_order_status;
--rollback DROP INDEX IF EXISTS idx_inventory_reservations_order;
--rollback DROP INDEX IF EXISTS idx_inventory_reservations_hold_expiry;
--rollback DROP INDEX IF EXISTS idx_booking_orders_hold_expires_at;
--rollback DROP INDEX IF EXISTS idx_booking_orders_departure_status;
--rollback DROP INDEX IF EXISTS idx_booking_orders_user_status;
--rollback DROP INDEX IF EXISTS idx_departure_inventory_hot_bucket;
--rollback DROP INDEX IF EXISTS idx_departure_inventory_sale_status;
--rollback DROP INDEX IF EXISTS idx_train_departures_route_time;
--rollback DROP INDEX IF EXISTS idx_train_departures_business_date;
--rollback DROP TABLE IF EXISTS processed_kafka_messages CASCADE;
--rollback DROP TABLE IF EXISTS outbox_events CASCADE;
--rollback DROP TABLE IF EXISTS payment_provider_callbacks CASCADE;
--rollback DROP TABLE IF EXISTS payment_transactions CASCADE;
--rollback DROP TABLE IF EXISTS inventory_reservations CASCADE;
--rollback DROP TABLE IF EXISTS booking_order_items CASCADE;
--rollback DROP TABLE IF EXISTS booking_orders CASCADE;
--rollback DROP TABLE IF EXISTS departure_inventory_buckets CASCADE;
--rollback DROP TABLE IF EXISTS train_departures CASCADE;
--rollback DROP TABLE IF EXISTS trains CASCADE;
--rollback DROP TABLE IF EXISTS train_route_stops CASCADE;
--rollback DROP TABLE IF EXISTS train_routes CASCADE;
--rollback DROP TABLE IF EXISTS railway_stations CASCADE;
