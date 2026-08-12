
package com.vetautet.app.domain.payment.model;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class PaymentTransaction {

    private final UUID paymentTransactionId;
    private final UUID bookingOrderId;
    private final PaymentMethodCode paymentMethodCode;
    private final String providerCode;
    private final String providerTransactionId;
    private final String providerPaymentUrl;
    private final BigDecimal amount;
    private final String currencyCode;
    private final PaymentTransactionStatus status;
    private final Instant requestedAt;
    private final Instant authorizedAt;
    private final Instant settledAt;
    private final Instant expiresAt;
    private final String failureCode;
    private final String failureMessage;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;


    public boolean isTerminal() {
        return status == PaymentTransactionStatus.SET
                || status == PaymentTransactionStatus.FAI
                || status == PaymentTransactionStatus.CNC
                || status == PaymentTransactionStatus.REF
                || status == PaymentTransactionStatus.EXP;
    }

}
