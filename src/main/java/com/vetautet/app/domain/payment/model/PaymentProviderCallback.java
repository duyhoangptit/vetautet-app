
package com.vetautet.app.domain.payment.model;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class PaymentProviderCallback {
    private final UUID paymentCallbackId;
    private final UUID paymentTransactionId;
    private final String providerCode;
    private final String providerEventId;
    private final PaymentCallbackStatus callbackStatus;
    private final String payload;
    private final Instant receivedAt;
    private final Instant processedAt;
    private final String errorMessage;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;
}
