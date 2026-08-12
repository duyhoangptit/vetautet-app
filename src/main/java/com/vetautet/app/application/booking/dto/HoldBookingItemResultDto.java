package com.vetautet.app.application.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class HoldBookingItemResultDto {
    private UUID bookingOrderItemId;
    private UUID inventoryBucketId;
    private Integer lineNo;
    private String seatClassCode;
    private String quotaCode;
    private Integer quantity;
    private BigDecimal unitPriceAmount;
    private BigDecimal lineTotalAmount;
}