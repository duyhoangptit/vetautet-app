package com.vetautet.app.presentation.rest.mapper;

import com.vetautet.app.application.ordersdemo.dto.OrderKeysetPage;
import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.presentation.rest.dto.response.OrderKeysetPageResponse;
import com.vetautet.app.presentation.rest.dto.response.OrderResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderPresentationMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .quantity(order.getQuantity())
                .paymentMethod(order.getPaymentMethod())
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderKeysetPageResponse toPageResponse(OrderKeysetPage page) {
        return OrderKeysetPageResponse.builder()
                .content(page.content().stream().map(this::toResponse).toList())
                .nextCursor(page.nextCursor())
                .prevCursor(page.prevCursor())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
