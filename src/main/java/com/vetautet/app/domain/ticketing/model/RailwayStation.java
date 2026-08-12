package com.vetautet.app.domain.ticketing.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class RailwayStation {

    private final UUID stationId;
    private final String stationCode;
    private final String stationName;
    private final String cityCode;
    private final String timezoneName;
    private final Integer displayOrder;
    private final StationStatus status;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;
}