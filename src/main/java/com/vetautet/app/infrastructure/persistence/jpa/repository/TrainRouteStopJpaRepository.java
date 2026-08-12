package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.infrastructure.persistence.jpa.entity.TrainRouteStopJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrainRouteStopJpaRepository extends JpaRepository<TrainRouteStopJpaEntity, UUID> {

    List<TrainRouteStopJpaEntity> findByRouteIdOrderByStopSequenceAsc(UUID routeId);

    List<TrainRouteStopJpaEntity> findByStationId(UUID stationId);
}