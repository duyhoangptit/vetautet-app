
package com.vetautet.app.domain.ticketing.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class TrainRouteStop {

    private final UUID routeStopId;
    private final UUID routeId;
    private final UUID stationId;
    private final Integer stopSequence;
    private final BigDecimal distanceFromOriginKm;
    private final Integer plannedArrivalOffsetMinutes;
    private final Integer plannedDepartureOffsetMinutes;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;
}