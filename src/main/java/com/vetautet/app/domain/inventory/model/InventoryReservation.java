package com.vetautet.app.domain.inventory.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class InventoryReservation {

    private final UUID inventoryReservationId;
    private final UUID bookingOrderId;
    private final UUID inventoryBucketId;
    private final Integer reservedQuantity;
    private final Instant holdExpiresAt;
    private final InventoryReservationStatus reservationStatus;
    private final Instant confirmedAt;
    private final Instant releasedAt;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;

    public boolean isHoldingAt(Instant currentTime) {
        return reservationStatus == InventoryReservationStatus.HLD
                && holdExpiresAt != null
                && holdExpiresAt.isAfter(currentTime);
    }
}