
package com.vetautet.app.presentation.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartureBucketAvailabilityResponse {
    private UUID inventoryBucketId;
    private String seatClassCode;
    private String quotaCode;
    private Short bucketNo;
    private Integer availableQuantity;
    private BigDecimal fareAmount;
    private String currencyCode;
}