package com.vetautet.app.application.ordersdemo.usecase;

import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FakeOrderDataFactoryTest {

    private final FakeOrderDataFactory factory = new FakeOrderDataFactory();

    @Test
    void generate_usesGivenIdAndCreatedAt() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-06-01T00:00:00Z");

        Order order = factory.generate(id, createdAt);

        assertThat(order.getId()).isEqualTo(id);
        assertThat(order.getCreatedAt()).isEqualTo(createdAt);
        assertThat(order.getUpdatedAt()).isEqualTo(createdAt);
    }

    @Test
    void generate_populatesAllRequiredFieldsNonBlank() {
        Order order = factory.generate(UUID.randomUUID(), Instant.now());

        assertThat(order.getOrderCode()).isNotBlank().contains("ORD-");
        assertThat(order.getCustomerName()).isNotBlank();
        assertThat(order.getCustomerEmail()).isNotBlank().contains("@");
        assertThat(order.getCustomerPhone()).isNotBlank();
        assertThat(order.getStatus()).isIn((Object[]) OrderStatus.values());
        assertThat(order.getTotalAmount().signum()).isPositive();
        assertThat(order.getCurrency()).isNotBlank();
        assertThat(order.getQuantity()).isPositive();
        assertThat(order.getPaymentMethod()).isNotBlank();
        assertThat(order.getShippingAddress()).isNotBlank();
        assertThat(order.getShippingCity()).isNotBlank();
    }

    @Test
    void generate_calledTwice_producesDifferentOrderCodesForDifferentIds() {
        Order first = factory.generate(UUID.randomUUID(), Instant.now());
        Order second = factory.generate(UUID.randomUUID(), Instant.now());

        assertThat(first.getOrderCode()).isNotEqualTo(second.getOrderCode());
    }
}
