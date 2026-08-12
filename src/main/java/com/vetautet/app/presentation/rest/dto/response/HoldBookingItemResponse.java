
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
public class HoldBookingItemResponse {
    private UUID bookingOrderItemId;
    private UUID inventoryBucketId;
    private Integer lineNo;
    private String seatClassCode;
    private String quotaCode;
    private Integer quantity;
    private BigDecimal unitPriceAmount;
    private BigDecimal lineTotalAmount;
}