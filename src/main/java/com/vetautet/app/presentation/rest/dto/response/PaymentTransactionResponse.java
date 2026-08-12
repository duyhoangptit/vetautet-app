
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
public class PaymentTransactionResponse {
    private UUID paymentTransactionId;
    private String paymentMethodCode;
    private String providerCode;
    private String providerTransactionId;
    private String providerPaymentUrl;
    private BigDecimal amount;
    private String currencyCode;
    private String status;
    private Instant requestedAt;
    private Instant settledAt;
    private Instant expiresAt;
}