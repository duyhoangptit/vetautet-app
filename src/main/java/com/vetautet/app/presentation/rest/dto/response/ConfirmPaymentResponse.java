
package com.vetautet.app.presentation.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentResponse {
    private UUID bookingOrderId;
    private String orderCode;
    private String bookingStatus;
    private Instant bookingConfirmedAt;
    private PaymentTransactionResponse paymentTransaction;
}
