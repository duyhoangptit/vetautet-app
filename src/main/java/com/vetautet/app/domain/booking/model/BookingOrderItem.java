package com.vetautet.app.domain.booking.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class BookingOrderItem {

    private final UUID bookingOrderItemId;
    private final UUID bookingOrderId;
    private final Integer lineNo;
    private final UUID inventoryBucketId;
    private final Integer travelFromStopSequence;
    private final Integer travelToStopSequence;
    private final String seatClassCode;
    private final String quotaCode;
    private final Integer quantity;
    private final BigDecimal unitPriceAmount;
    private final BigDecimal lineTotalAmount;
    private final BookingOrderItemStatus itemStatus;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;
}