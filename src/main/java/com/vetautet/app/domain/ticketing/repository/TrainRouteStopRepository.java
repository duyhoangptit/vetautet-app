
package com.vetautet.app.domain.ticketing.repository;

import com.vetautet.app.domain.ticketing.model.TrainRouteStop;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainRouteStopRepository {

    TrainRouteStop save(TrainRouteStop trainRouteStop);

    Optional<TrainRouteStop> findById(UUID routeStopId);

    List<TrainRouteStop> findByRouteId(UUID routeId);

    List<TrainRouteStop> findByStationId(UUID stationId);
}