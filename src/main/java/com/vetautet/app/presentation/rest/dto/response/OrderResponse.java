package com.vetautet.app.presentation.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID id;
    private String orderCode;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
    private Integer quantity;
    private String paymentMethod;
    private String shippingAddress;
    private String shippingCity;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
