
package com.vetautet.app.domain.ticketing.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class TrainDeparture {

    private final UUID departureId;
    private final UUID routeId;
    private final UUID trainId;
    private final String departureCode;
    private final LocalDate businessDate;
    private final UUID originStationId;
    private final UUID destinationStationId;
    private final Instant plannedDepartureAt;
    private final Instant plannedArrivalAt;
    private final Instant saleOpensAt;
    private final Instant saleClosesAt;
    private final TrainDepartureStatus status;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;
}