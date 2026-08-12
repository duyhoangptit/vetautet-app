package com.vetautet.app.application.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ConfirmPaymentCommand {
    private UUID paymentTransactionId;
    private String providerTransactionId;
    private String providerPaymentUrl;
    private Instant confirmedAt;
}