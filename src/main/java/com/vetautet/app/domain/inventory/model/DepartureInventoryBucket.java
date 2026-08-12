package com.vetautet.app.domain.inventory.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class DepartureInventoryBucket {

    private final UUID inventoryBucketId;
    private final UUID departureId;
    private final String seatClassCode;
    private final String quotaCode;
    private final Short bucketNo;
    private final Integer totalQuantity;
    private final Integer availableQuantity;
    private final Integer reservedQuantity;
    private final Integer soldQuantity;
    private final Integer oversellLimit;
    private final BigDecimal fareAmount;
    private final String currencyCode;
    private final InventorySaleStatus saleStatus;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;

    public boolean hasAvailableQuantity() {
        return availableQuantity != null && availableQuantity > 0;
    }
}