
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.inventory.model.DepartureInventoryBucket;
import com.vetautet.app.infrastructure.persistence.jpa.entity.DepartureInventoryBucketJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class DepartureInventoryBucketEntityMapper {

    public DepartureInventoryBucket toDomain(DepartureInventoryBucketJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return DepartureInventoryBucket.builder()
                .inventoryBucketId(entity.getInventoryBucketId())
                .departureId(entity.getDepartureId())
                .seatClassCode(entity.getSeatClassCode())
                .quotaCode(entity.getQuotaCode())
                .bucketNo(entity.getBucketNo())
                .totalQuantity(entity.getTotalQuantity())
                .availableQuantity(entity.getAvailableQuantity())
                .reservedQuantity(entity.getReservedQuantity())
                .soldQuantity(entity.getSoldQuantity())
                .oversellLimit(entity.getOversellLimit())
                .fareAmount(entity.getFareAmount())
                .currencyCode(entity.getCurrencyCode())
                .saleStatus(entity.getSaleStatus())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public DepartureInventoryBucketJpaEntity toEntity(DepartureInventoryBucket domain) {
        if (domain == null) {
            return null;
        }

        DepartureInventoryBucketJpaEntity entity = DepartureInventoryBucketJpaEntity.builder()
                .inventoryBucketId(domain.getInventoryBucketId())
                .departureId(domain.getDepartureId())
                .seatClassCode(domain.getSeatClassCode())
                .quotaCode(domain.getQuotaCode())
                .bucketNo(domain.getBucketNo())
                .totalQuantity(domain.getTotalQuantity())
                .availableQuantity(domain.getAvailableQuantity())
                .reservedQuantity(domain.getReservedQuantity())
                .soldQuantity(domain.getSoldQuantity())
                .oversellLimit(domain.getOversellLimit())
                .fareAmount(domain.getFareAmount())
                .currencyCode(domain.getCurrencyCode())
                .saleStatus(domain.getSaleStatus())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }
}