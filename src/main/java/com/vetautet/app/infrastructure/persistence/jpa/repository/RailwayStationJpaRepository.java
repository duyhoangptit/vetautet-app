package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.domain.ticketing.model.StationStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.RailwayStationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RailwayStationJpaRepository extends JpaRepository<RailwayStationJpaEntity, UUID> {

    Optional<RailwayStationJpaEntity> findByStationCode(String stationCode);

    List<RailwayStationJpaEntity> findByStatusOrderByDisplayOrderAsc(StationStatus status);
}