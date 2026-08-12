package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.ticketing.model.Train;
import com.vetautet.app.domain.ticketing.model.TrainStatus;
import com.vetautet.app.domain.ticketing.repository.TrainRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.TrainJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.TrainEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class TrainRepositoryAdapter implements TrainRepository {

    private final TrainJpaRepository jpaRepository;
    private final TrainEntityMapper mapper;

    @Override
    public Train save(Train train) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(train)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Train> findById(UUID trainId) {
        return jpaRepository.findById(trainId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Train> findByCode(String trainCode) {
        return jpaRepository.findByTrainCode(trainCode).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Train> findByStatus(TrainStatus status) {
        return jpaRepository.findByStatus(status).stream().map(mapper::toDomain).toList();
    }
}