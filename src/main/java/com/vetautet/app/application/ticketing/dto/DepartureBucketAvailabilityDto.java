package com.vetautet.app.application.ticketing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class DepartureBucketAvailabilityDto {
    private UUID inventoryBucketId;
    private String seatClassCode;
    private String quotaCode;
    private Short bucketNo;
    private Integer availableQuantity;
    private BigDecimal fareAmount;
    private String currencyCode;
}