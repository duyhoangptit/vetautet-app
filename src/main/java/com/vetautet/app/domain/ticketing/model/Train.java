
package com.vetautet.app.domain.ticketing.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class Train {

    private final UUID trainId;
    private final String trainCode;
    private final String trainName;
    private final String seatLayoutVersion;
    private final String operatorCode;
    private final TrainStatus status;
    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;
}