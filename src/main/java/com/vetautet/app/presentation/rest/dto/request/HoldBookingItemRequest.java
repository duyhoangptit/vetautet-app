
package com.vetautet.app.presentation.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldBookingItemRequest {

    @NotNull(message = "Inventory bucket ID is required")
    private UUID inventoryBucketId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than zero")
    private Integer quantity;

    @NotNull(message = "Travel from stop sequence is required")
    @Min(value = 1, message = "Travel from stop sequence must be greater than zero")
    private Integer travelFromStopSequence;

    @NotNull(message = "Travel to stop sequence is required")
    @Min(value = 1, message = "Travel to stop sequence must be greater than zero")
    private Integer travelToStopSequence;
}