
package com.vetautet.app.presentation.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartureAvailabilityResponse {
    private UUID departureId;
    private String departureCode;
    private LocalDate businessDate;
    private UUID originStationId;
    private UUID destinationStationId;
    private Instant plannedDepartureAt;
    private Instant plannedArrivalAt;
    private List<DepartureBucketAvailabilityResponse> buckets;
}