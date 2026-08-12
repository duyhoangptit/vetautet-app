
package com.vetautet.app.presentation.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldBookingResponse {
    private UUID bookingOrderId;
    private String orderCode;
    private String status;
    private BigDecimal totalAmount;
    private String currencyCode;
    private Instant holdExpiresAt;
    private List<HoldBookingItemResponse> items;
    private PaymentTransactionResponse paymentTransaction;
}
