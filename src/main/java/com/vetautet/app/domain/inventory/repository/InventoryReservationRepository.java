package com.vetautet.app.domain.inventory.repository;

import com.vetautet.app.domain.inventory.model.InventoryReservation;
import com.vetautet.app.domain.inventory.model.InventoryReservationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository {

    InventoryReservation save(InventoryReservation inventoryReservation);

    Optional<InventoryReservation> findById(UUID inventoryReservationId);

    List<InventoryReservation> findByBookingOrderIdAndStatus(UUID bookingOrderId, InventoryReservationStatus reservationStatus);

    List<InventoryReservation> findByInventoryBucketId(UUID inventoryBucketId);

    List<InventoryReservation> findExpiredReservations(Instant holdExpiresAt);
}