package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.inventory.model.InventoryReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations", indexes = {
        @Index(name = "idx_inventory_reservations_hold_expiry", columnList = "hold_expires_at,reservation_status"),
        @Index(name = "idx_inventory_reservations_order", columnList = "booking_order_id,reservation_status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "inventory_reservation_id", nullable = false, updatable = false)
    private UUID inventoryReservationId;

    @Column(name = "booking_order_id", nullable = false)
    private UUID bookingOrderId;

    @Column(name = "inventory_bucket_id", nullable = false)
    private UUID inventoryBucketId;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "hold_expires_at", nullable = false)
    private Instant holdExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false, length = 3)
    private InventoryReservationStatus reservationStatus;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "released_at")
    private Instant releasedAt;
}


