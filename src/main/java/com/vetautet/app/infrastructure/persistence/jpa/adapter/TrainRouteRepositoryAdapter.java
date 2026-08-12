package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.ticketing.model.TrainRoute;
import com.vetautet.app.domain.ticketing.model.TrainRouteStatus;
import com.vetautet.app.domain.ticketing.repository.TrainRouteRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.TrainRouteJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.TrainRouteEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class TrainRouteRepositoryAdapter implements TrainRouteRepository {

    private final TrainRouteJpaRepository jpaRepository;
    private final TrainRouteEntityMapper mapper;

    @Override
    public TrainRoute save(TrainRoute trainRoute) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(trainRoute)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrainRoute> findById(UUID routeId) {
        return jpaRepository.findById(routeId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrainRoute> findByCode(String routeCode) {
        return jpaRepository.findByRouteCode(routeCode).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainRoute> findByStatus(TrainRouteStatus status) {
        return jpaRepository.findByStatus(status).stream().map(mapper::toDomain).toList();
    }
}