package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.domain.inventory.model.InventoryReservationStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.InventoryReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryReservationJpaRepository extends JpaRepository<InventoryReservationJpaEntity, UUID> {

    List<InventoryReservationJpaEntity> findByBookingOrderIdAndReservationStatus(UUID bookingOrderId, InventoryReservationStatus reservationStatus);

    List<InventoryReservationJpaEntity> findByInventoryBucketId(UUID inventoryBucketId);

    List<InventoryReservationJpaEntity> findByReservationStatusAndHoldExpiresAtBefore(InventoryReservationStatus reservationStatus, Instant holdExpiresAt);
}