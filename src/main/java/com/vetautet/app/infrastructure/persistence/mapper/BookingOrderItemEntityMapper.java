
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.booking.model.BookingOrderItem;
import com.vetautet.app.infrastructure.persistence.jpa.entity.BookingOrderItemJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class BookingOrderItemEntityMapper {

    public BookingOrderItem toDomain(BookingOrderItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return BookingOrderItem.builder()
                .bookingOrderItemId(entity.getBookingOrderItemId())
                .bookingOrderId(entity.getBookingOrderId())
                .lineNo(entity.getLineNo())
                .inventoryBucketId(entity.getInventoryBucketId())
                .travelFromStopSequence(entity.getTravelFromStopSequence())
                .travelToStopSequence(entity.getTravelToStopSequence())
                .seatClassCode(entity.getSeatClassCode())
                .quotaCode(entity.getQuotaCode())
                .quantity(entity.getQuantity())
                .unitPriceAmount(entity.getUnitPriceAmount())
                .lineTotalAmount(entity.getLineTotalAmount())
                .itemStatus(entity.getItemStatus())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public BookingOrderItemJpaEntity toEntity(BookingOrderItem domain) {
        if (domain == null) {
            return null;
        }

        BookingOrderItemJpaEntity entity = BookingOrderItemJpaEntity.builder()
                .bookingOrderItemId(domain.getBookingOrderItemId())
                .bookingOrderId(domain.getBookingOrderId())
                .lineNo(domain.getLineNo())
                .inventoryBucketId(domain.getInventoryBucketId())
                .travelFromStopSequence(domain.getTravelFromStopSequence())
                .travelToStopSequence(domain.getTravelToStopSequence())
                .seatClassCode(domain.getSeatClassCode())
                .quotaCode(domain.getQuotaCode())
                .quantity(domain.getQuantity())
                .unitPriceAmount(domain.getUnitPriceAmount())
                .lineTotalAmount(domain.getLineTotalAmount())
                .itemStatus(domain.getItemStatus())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }
}