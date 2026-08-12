
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.inventory.model.InventoryReservation;
import com.vetautet.app.infrastructure.persistence.jpa.entity.InventoryReservationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class InventoryReservationEntityMapper {

    public InventoryReservation toDomain(InventoryReservationJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return InventoryReservation.builder()
                .inventoryReservationId(entity.getInventoryReservationId())
                .bookingOrderId(entity.getBookingOrderId())
                .inventoryBucketId(entity.getInventoryBucketId())
                .reservedQuantity(entity.getReservedQuantity())
                .holdExpiresAt(entity.getHoldExpiresAt())
                .reservationStatus(entity.getReservationStatus())
                .confirmedAt(entity.getConfirmedAt())
                .releasedAt(entity.getReleasedAt())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public InventoryReservationJpaEntity toEntity(InventoryReservation domain) {
        if (domain == null) {
            return null;
        }

        InventoryReservationJpaEntity entity = InventoryReservationJpaEntity.builder()
                .inventoryReservationId(domain.getInventoryReservationId())
                .bookingOrderId(domain.getBookingOrderId())
                .inventoryBucketId(domain.getInventoryBucketId())
                .reservedQuantity(domain.getReservedQuantity())
                .holdExpiresAt(domain.getHoldExpiresAt())
                .reservationStatus(domain.getReservationStatus())
                .confirmedAt(domain.getConfirmedAt())
                .releasedAt(domain.getReleasedAt())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }
}