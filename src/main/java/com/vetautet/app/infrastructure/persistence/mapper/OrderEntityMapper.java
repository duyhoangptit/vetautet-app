package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.infrastructure.persistence.jpa.entity.OrderJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderEntityMapper {

    public Order toDomain(OrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Order.builder()
                .id(entity.getId())
                .orderCode(entity.getOrderCode())
                .customerName(entity.getCustomerName())
                .customerEmail(entity.getCustomerEmail())
                .customerPhone(entity.getCustomerPhone())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .quantity(entity.getQuantity())
                .paymentMethod(entity.getPaymentMethod())
                .shippingAddress(entity.getShippingAddress())
                .shippingCity(entity.getShippingCity())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public OrderJpaEntity toEntity(Order domain) {
        if (domain == null) {
            return null;
        }

        return OrderJpaEntity.builder()
                .id(domain.getId())
                .orderCode(domain.getOrderCode())
                .customerName(domain.getCustomerName())
                .customerEmail(domain.getCustomerEmail())
                .customerPhone(domain.getCustomerPhone())
                .status(domain.getStatus())
                .totalAmount(domain.getTotalAmount())
                .currency(domain.getCurrency())
                .quantity(domain.getQuantity())
                .paymentMethod(domain.getPaymentMethod())
                .shippingAddress(domain.getShippingAddress())
                .shippingCity(domain.getShippingCity())
                .notes(domain.getNotes())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
