package com.vetautet.app.application.booking.dto;

import com.vetautet.app.domain.payment.model.PaymentMethodCode;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class HoldBookingCommand {
    private UUID userId;
    private UUID departureId;
    private String bookingChannel;
    private String customerFullName;
    private String customerEmail;
    private String customerPhone;
    private PaymentMethodCode paymentMethodCode;
    private String providerCode;
    private Integer holdDurationMinutes;
    private String idempotencyKey;
    private List<HoldBookingItemCommand> items;
}