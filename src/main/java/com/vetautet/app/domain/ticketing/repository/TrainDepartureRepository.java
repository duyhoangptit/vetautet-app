
package com.vetautet.app.domain.ticketing.repository;

import com.vetautet.app.domain.ticketing.model.TrainDeparture;
import com.vetautet.app.domain.ticketing.model.TrainDepartureStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainDepartureRepository {

    TrainDeparture save(TrainDeparture trainDeparture);

    Optional<TrainDeparture> findById(UUID departureId);

    Optional<TrainDeparture> findByCode(String departureCode);

    List<TrainDeparture> findByBusinessDateAndStatusAndOriginStationIdAndDestinationStationId(
            LocalDate businessDate, TrainDepartureStatus status, UUID originStationId, UUID destinationStationId);

    List<TrainDeparture> findUpcomingByRouteId(UUID routeId, Instant plannedDepartureAt);
}