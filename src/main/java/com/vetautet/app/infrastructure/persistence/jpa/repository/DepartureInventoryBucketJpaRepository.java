package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.domain.inventory.model.InventorySaleStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.DepartureInventoryBucketJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartureInventoryBucketJpaRepository extends JpaRepository<DepartureInventoryBucketJpaEntity, UUID> {

    List<DepartureInventoryBucketJpaEntity> findByDepartureIdAndSaleStatus(UUID departureId, InventorySaleStatus saleStatus);

    Optional<DepartureInventoryBucketJpaEntity> findByDepartureIdAndSeatClassCodeAndQuotaCodeAndBucketNo(
            UUID departureId,
            String seatClassCode,
            String quotaCode,
            Short bucketNo);

    List<DepartureInventoryBucketJpaEntity> findByDepartureIdOrderBySeatClassCodeAscQuotaCodeAscBucketNoAsc(UUID departureId);
}