package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.ticketing.model.TrainRouteStop;
import com.vetautet.app.domain.ticketing.repository.TrainRouteStopRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.TrainRouteStopJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.TrainRouteStopEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class TrainRouteStopRepositoryAdapter implements TrainRouteStopRepository {

    private final TrainRouteStopJpaRepository jpaRepository;
    private final TrainRouteStopEntityMapper mapper;

    @Override
    public TrainRouteStop save(TrainRouteStop trainRouteStop) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(trainRouteStop)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrainRouteStop> findById(UUID routeStopId) {
        return jpaRepository.findById(routeStopId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainRouteStop> findByRouteId(UUID routeId) {
        return jpaRepository.findByRouteIdOrderByStopSequenceAsc(routeId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainRouteStop> findByStationId(UUID stationId) {
        return jpaRepository.findByStationId(stationId).stream().map(mapper::toDomain).toList();
    }
}