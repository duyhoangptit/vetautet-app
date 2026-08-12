
package com.vetautet.app.domain.ticketing.repository;

import com.vetautet.app.domain.ticketing.model.TrainRoute;
import com.vetautet.app.domain.ticketing.model.TrainRouteStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainRouteRepository {

    TrainRoute save(TrainRoute trainRoute);

    Optional<TrainRoute> findById(UUID routeId);

    Optional<TrainRoute> findByCode(String routeCode);

    List<TrainRoute> findByStatus(TrainRouteStatus status);
}