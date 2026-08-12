package com.vetautet.app.domain.booking.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class BookingOrder {

    private final UUID bookingOrderId;
    private final String orderCode;
    private final UUID userId;
    private final UUID departureId;
    private final String bookingChannel;
    private final String customerFullName;
    private final String customerEmail;
    private final String customerPhone;
    private final BookingOrderStatus status;
    private final BigDecimal totalAmount;
    private final String currencyCode;
    private final Instant holdExpiresAt;
    private final Instant confirmedAt;
    private final Instant cancelledAt;
    private final String idempotencyKey;
    private final String failureReason;
    private final List<BookingOrderItem> items;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;

    public boolean isActiveHoldAt(Instant currentTime) {
        return status == BookingOrderStatus.HLD && holdExpiresAt != null && holdExpiresAt.isAfter(currentTime);
    }

    public boolean isTerminal() {
        return status == BookingOrderStatus.CNF
                || status == BookingOrderStatus.EXP
                || status == BookingOrderStatus.CXL
                || status == BookingOrderStatus.FAI
                || status == BookingOrderStatus.RFD;
    }
}