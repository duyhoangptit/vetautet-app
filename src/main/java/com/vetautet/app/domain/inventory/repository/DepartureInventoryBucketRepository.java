package com.vetautet.app.domain.inventory.repository;

import com.vetautet.app.domain.inventory.model.DepartureInventoryBucket;
import com.vetautet.app.domain.inventory.model.InventorySaleStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartureInventoryBucketRepository {

    DepartureInventoryBucket save(DepartureInventoryBucket inventoryBucket);

    Optional<DepartureInventoryBucket> findById(UUID inventoryBucketId);

    Optional<DepartureInventoryBucket> findByDepartureAndBucketNo(
            UUID departureId,
            String seatClassCode,
            String quotaCode,
            Short bucketNo);

    List<DepartureInventoryBucket> findByDepartureId(UUID departureId);

    List<DepartureInventoryBucket> findByDepartureIdAndSaleStatus(UUID departureId, InventorySaleStatus saleStatus);
}