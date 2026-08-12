package com.vetautet.app.application.payment.dto;

import com.vetautet.app.domain.payment.model.PaymentMethodCode;
import com.vetautet.app.domain.payment.model.PaymentTransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PaymentTransactionDto {
    private UUID paymentTransactionId;
    private PaymentMethodCode paymentMethodCode;
    private String providerCode;
    private String providerTransactionId;
    private String providerPaymentUrl;
    private BigDecimal amount;
    private String currencyCode;
    private PaymentTransactionStatus status;
    private Instant requestedAt;
    private Instant settledAt;
    private Instant expiresAt;
}