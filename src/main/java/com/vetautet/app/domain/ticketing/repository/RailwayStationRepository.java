package com.vetautet.app.domain.ticketing.repository;

import com.vetautet.app.domain.ticketing.model.RailwayStation;
import com.vetautet.app.domain.ticketing.model.StationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RailwayStationRepository {

    RailwayStation save(RailwayStation railwayStation);

    Optional<RailwayStation> findById(UUID stationId);

    Optional<RailwayStation> findByCode(String stationCode);

    List<RailwayStation> findByStatus(StationStatus status);
}