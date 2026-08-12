package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.ticketing.model.TrainDeparture;
import com.vetautet.app.domain.ticketing.model.TrainDepartureStatus;
import com.vetautet.app.domain.ticketing.repository.TrainDepartureRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.TrainDepartureJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.TrainDepartureEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class TrainDepartureRepositoryAdapter implements TrainDepartureRepository {

    private final TrainDepartureJpaRepository jpaRepository;
    private final TrainDepartureEntityMapper mapper;

    @Override
    public TrainDeparture save(TrainDeparture trainDeparture) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(trainDeparture)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrainDeparture> findById(UUID departureId) {
        return jpaRepository.findById(departureId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrainDeparture> findByCode(String departureCode) {
        return jpaRepository.findByDepartureCode(departureCode).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainDeparture> findByBusinessDateAndStatus(LocalDate businessDate, TrainDepartureStatus status) {
        return jpaRepository.findByBusinessDateAndStatus(businessDate, status).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainDeparture> findUpcomingByRouteId(UUID routeId, Instant plannedDepartureAt) {
        return jpaRepository.findByRouteIdAndPlannedDepartureAtAfter(routeId, plannedDepartureAt).stream().map(mapper::toDomain).toList();
    }
}