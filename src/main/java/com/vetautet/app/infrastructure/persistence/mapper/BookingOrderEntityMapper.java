
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.booking.model.BookingOrder;
import com.vetautet.app.domain.booking.model.BookingOrderItem;
import com.vetautet.app.infrastructure.persistence.jpa.entity.BookingOrderJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class BookingOrderEntityMapper {

    private final BookingOrderItemEntityMapper itemEntityMapper;

    public BookingOrderEntityMapper(BookingOrderItemEntityMapper itemEntityMapper) {
        this.itemEntityMapper = itemEntityMapper;
    }

    public BookingOrder toDomain(BookingOrderJpaEntity entity) {
        return toDomain(entity, Collections.emptyList());
    }

    public BookingOrder toDomain(BookingOrderJpaEntity entity, List<BookingOrderItem> items) {
        if (entity == null) {
            return null;
        }

        return BookingOrder.builder()
                .bookingOrderId(entity.getBookingOrderId())
                .orderCode(entity.getOrderCode())
                .userId(entity.getUserId())
                .departureId(entity.getDepartureId())
                .bookingChannel(entity.getBookingChannel())
                .customerFullName(entity.getCustomerFullName())
                .customerEmail(entity.getCustomerEmail())
                .customerPhone(entity.getCustomerPhone())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .currencyCode(entity.getCurrencyCode())
                .holdExpiresAt(entity.getHoldExpiresAt())
                .confirmedAt(entity.getConfirmedAt())
                .cancelledAt(entity.getCancelledAt())
                .idempotencyKey(entity.getIdempotencyKey())
                .failureReason(entity.getFailureReason())
                .items(items)
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public BookingOrderJpaEntity toEntity(BookingOrder domain) {
        if (domain == null) {
            return null;
        }

        BookingOrderJpaEntity entity = BookingOrderJpaEntity.builder()
                .bookingOrderId(domain.getBookingOrderId())
                .orderCode(domain.getOrderCode())
                .userId(domain.getUserId())
                .departureId(domain.getDepartureId())
                .bookingChannel(domain.getBookingChannel())
                .customerFullName(domain.getCustomerFullName())
                .customerEmail(domain.getCustomerEmail())
                .customerPhone(domain.getCustomerPhone())
                .status(domain.getStatus())
                .totalAmount(domain.getTotalAmount())
                .currencyCode(domain.getCurrencyCode())
                .holdExpiresAt(domain.getHoldExpiresAt())
                .confirmedAt(domain.getConfirmedAt())
                .cancelledAt(domain.getCancelledAt())
                .idempotencyKey(domain.getIdempotencyKey())
                .failureReason(domain.getFailureReason())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }

    public List<BookingOrderItem> toDomainItems(List<com.vetautet.app.infrastructure.persistence.jpa.entity.BookingOrderItemJpaEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }

        return entities.stream().map(itemEntityMapper::toDomain).toList();
    }
}