package com.vetautet.app.application.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class HoldBookingItemCommand {
    private UUID inventoryBucketId;
    private Integer quantity;
    private Integer travelFromStopSequence;
    private Integer travelToStopSequence;
}