
package com.vetautet.app.domain.ticketing.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class TrainRoute {

    private final UUID routeId;
    private final String routeCode;
    private final String routeName;
    private final UUID originStationId;
    private final UUID destinationStationId;
    private final BigDecimal distanceKm;
    private final TrainRouteStatus status;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;
}