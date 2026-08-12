package com.vetautet.app.application.booking.dto;

import com.vetautet.app.application.payment.dto.PaymentTransactionDto;
import com.vetautet.app.domain.booking.model.BookingOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class HoldBookingResultDto {
    private UUID bookingOrderId;
    private String orderCode;
    private BookingOrderStatus status;
    private BigDecimal totalAmount;
    private String currencyCode;
    private Instant holdExpiresAt;
    private List<HoldBookingItemResultDto> items;
    private PaymentTransactionDto paymentTransaction;
}