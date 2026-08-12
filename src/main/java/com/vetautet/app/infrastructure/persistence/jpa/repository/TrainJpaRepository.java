package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.domain.ticketing.model.TrainStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.TrainJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainJpaRepository extends JpaRepository<TrainJpaEntity, UUID> {

    Optional<TrainJpaEntity> findByTrainCode(String trainCode);

    List<TrainJpaEntity> findByStatus(TrainStatus status);
}