package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.ticketing.model.RailwayStation;
import com.vetautet.app.domain.ticketing.model.StationStatus;
import com.vetautet.app.domain.ticketing.repository.RailwayStationRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.RailwayStationJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.RailwayStationEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class RailwayStationRepositoryAdapter implements RailwayStationRepository {

    private final RailwayStationJpaRepository jpaRepository;
    private final RailwayStationEntityMapper mapper;

    @Override
    public RailwayStation save(RailwayStation railwayStation) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(railwayStation)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RailwayStation> findById(UUID stationId) {
        return jpaRepository.findById(stationId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RailwayStation> findByCode(String stationCode) {
        return jpaRepository.findByStationCode(stationCode).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RailwayStation> findByStatus(StationStatus status) {
        return jpaRepository.findByStatusOrderByDisplayOrderAsc(status).stream().map(mapper::toDomain).toList();
    }
}