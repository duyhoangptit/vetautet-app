package com.vetautet.app.application.payment.dto;

import com.vetautet.app.domain.booking.model.BookingOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ConfirmPaymentResultDto {
    private UUID bookingOrderId;
    private String orderCode;
    private BookingOrderStatus bookingStatus;
    private Instant bookingConfirmedAt;
    private PaymentTransactionDto paymentTransaction;
}