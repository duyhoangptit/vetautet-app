package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.inventory.model.DepartureInventoryBucket;
import com.vetautet.app.domain.inventory.model.InventorySaleStatus;
import com.vetautet.app.domain.inventory.repository.DepartureInventoryBucketRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.DepartureInventoryBucketJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.DepartureInventoryBucketEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class DepartureInventoryBucketRepositoryAdapter implements DepartureInventoryBucketRepository {

    private final DepartureInventoryBucketJpaRepository jpaRepository;
    private final DepartureInventoryBucketEntityMapper mapper;

    @Override
    public DepartureInventoryBucket save(DepartureInventoryBucket inventoryBucket) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(inventoryBucket)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DepartureInventoryBucket> findById(UUID inventoryBucketId) {
        return jpaRepository.findById(inventoryBucketId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DepartureInventoryBucket> findByDepartureAndBucketNo(UUID departureId, String seatClassCode, String quotaCode, Short bucketNo) {
        return jpaRepository.findByDepartureIdAndSeatClassCodeAndQuotaCodeAndBucketNo(departureId, seatClassCode, quotaCode, bucketNo)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartureInventoryBucket> findByDepartureId(UUID departureId) {
        return jpaRepository.findByDepartureIdOrderBySeatClassCodeAscQuotaCodeAscBucketNoAsc(departureId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartureInventoryBucket> findByDepartureIdAndSaleStatus(UUID departureId, InventorySaleStatus saleStatus) {
        return jpaRepository.findByDepartureIdAndSaleStatus(departureId, saleStatus)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}