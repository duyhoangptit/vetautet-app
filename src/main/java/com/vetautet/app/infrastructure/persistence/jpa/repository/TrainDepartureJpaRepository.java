package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.domain.ticketing.model.TrainDepartureStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.TrainDepartureJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainDepartureJpaRepository extends JpaRepository<TrainDepartureJpaEntity, UUID> {

    Optional<TrainDepartureJpaEntity> findByDepartureCode(String departureCode);

    List<TrainDepartureJpaEntity> findByBusinessDateAndStatusAndOriginStationIdAndDestinationStationId(
            LocalDate businessDate, TrainDepartureStatus status, UUID originStationId, UUID destinationStationId);

    List<TrainDepartureJpaEntity> findByRouteIdAndPlannedDepartureAtAfter(UUID routeId, Instant plannedDepartureAt);
}