package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.domain.ticketing.model.TrainRouteStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.TrainRouteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainRouteJpaRepository extends JpaRepository<TrainRouteJpaEntity, UUID> {

    Optional<TrainRouteJpaEntity> findByRouteCode(String routeCode);

    List<TrainRouteJpaEntity> findByStatus(TrainRouteStatus status);
}