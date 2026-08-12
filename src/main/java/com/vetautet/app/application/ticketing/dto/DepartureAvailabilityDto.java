package com.vetautet.app.application.ticketing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DepartureAvailabilityDto {
    private UUID departureId;
    private String departureCode;
    private LocalDate businessDate;
    private UUID originStationId;
    private UUID destinationStationId;
    private Instant plannedDepartureAt;
    private Instant plannedArrivalAt;
    private List<DepartureBucketAvailabilityDto> buckets;
}