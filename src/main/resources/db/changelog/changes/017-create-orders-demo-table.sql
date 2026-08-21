--liquibase formatted sql

--changeset vetautet-dev:017-create-orders-demo-table
--comment: Create the orders-demo table for the keyset-pagination experiment (docs/keyset-pagination-spec.md). id is UUIDv7, generated in application code (Uuid7Generator) - its embedded timestamp makes it monotonically increasing and unique on its own, so it doubles as the sole cursor sort key/tie-breaker. No composite (created_at, id) index is needed: the primary key btree on id already serves "WHERE id < :cursor ORDER BY id DESC" / "WHERE id > :cursor ORDER BY id ASC" as an index-seek range scan. See docs/superpowers/specs/2026-08-21-orders-keyset-pagination-demo-design.md.
CREATE TABLE orders
(
    id               UUID                     NOT NULL,
    order_code       VARCHAR(40)              NOT NULL,
    customer_name    VARCHAR(150)             NOT NULL,
    customer_email   VARCHAR(150)             NOT NULL,
    customer_phone   VARCHAR(30),
    status           VARCHAR(20)              NOT NULL,
    total_amount     NUMERIC(18, 2)           NOT NULL,
    currency         VARCHAR(3)               NOT NULL,
    quantity         INT                      NOT NULL,
    payment_method   VARCHAR(30),
    shipping_address VARCHAR(255),
    shipping_city    VARCHAR(100),
    notes            VARCHAR(500),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uk_orders_code UNIQUE (order_code),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'PAID', 'CANCELLED', 'REFUNDED', 'COMPLETED'))
);

--rollback DROP TABLE IF EXISTS orders;
