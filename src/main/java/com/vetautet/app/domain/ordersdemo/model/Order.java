package com.vetautet.app.domain.ordersdemo.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Simulated order record for the keyset-pagination demo (see
 * docs/keyset-pagination-spec.md). Isolated from the real
 * {@code com.vetautet.app.domain.booking} model - not a business entity.
 */
@Getter
@Builder(toBuilder = true)
public class Order {

    private final UUID id;
    private final String orderCode;
    private final String customerName;
    private final String customerEmail;
    private final String customerPhone;
    private final OrderStatus status;
    private final BigDecimal totalAmount;
    private final String currency;
    private final Integer quantity;
    private final String paymentMethod;
    private final String shippingAddress;
    private final String shippingCity;
    private final String notes;
    private final Instant createdAt;
    private final Instant updatedAt;
}
