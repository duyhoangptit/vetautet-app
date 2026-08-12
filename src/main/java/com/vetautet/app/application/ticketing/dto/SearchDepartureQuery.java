package com.vetautet.app.application.ticketing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class SearchDepartureQuery {
    private UUID originStationId;
    private UUID destinationStationId;
    private LocalDate businessDate;
    private Instant currentTime;
}