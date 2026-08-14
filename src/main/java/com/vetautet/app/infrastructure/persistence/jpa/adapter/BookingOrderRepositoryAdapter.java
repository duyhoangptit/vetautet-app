package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vetautet.app.domain.booking.model.BookingOrder;
import com.vetautet.app.domain.booking.model.BookingOrderStatus;
import com.vetautet.app.domain.booking.repository.BookingOrderRepository;
import com.vetautet.app.infrastructure.persistence.jpa.entity.BookingOrderItemJpaEntity;
import com.vetautet.app.infrastructure.persistence.jpa.entity.BookingOrderJpaEntity;
import com.vetautet.app.infrastructure.persistence.jpa.repository.BookingOrderItemJpaRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.BookingOrderJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.BookingOrderEntityMapper;
import com.vetautet.app.infrastructure.persistence.mapper.BookingOrderItemEntityMapper;
import com.vetautet.app.shared.common.util.PageableSanitizer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class BookingOrderRepositoryAdapter implements BookingOrderRepository {

    private final BookingOrderJpaRepository bookingOrderJpaRepository;
    private final BookingOrderItemJpaRepository bookingOrderItemJpaRepository;
    private final BookingOrderEntityMapper bookingOrderEntityMapper;
    private final BookingOrderItemEntityMapper bookingOrderItemEntityMapper;

    @Override
    public BookingOrder save(BookingOrder bookingOrder) {
        BookingOrderJpaEntity orderEntity = bookingOrderJpaRepository.save(bookingOrderEntityMapper.toEntity(bookingOrder));
        syncItems(orderEntity.getBookingOrderId(), bookingOrder);
        return findById(orderEntity.getBookingOrderId()).orElseGet(() -> bookingOrderEntityMapper.toDomain(orderEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BookingOrder> findById(UUID bookingOrderId) {
        return bookingOrderJpaRepository.findById(bookingOrderId)
                .map(this::toAggregate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BookingOrder> findByOrderCode(String orderCode) {
        return bookingOrderJpaRepository.findByOrderCode(orderCode)
                .map(this::toAggregate);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByOrderCode(String orderCode) {
        return bookingOrderJpaRepository.existsByOrderCode(orderCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingOrder> findByUserId(UUID userId, Pageable pageable) {
        return bookingOrderJpaRepository.findByUserId(userId, PageableSanitizer.capped(pageable))
                .map(bookingOrderEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingOrder> findByUserIdAndStatus(UUID userId, BookingOrderStatus status, Pageable pageable) {
        return bookingOrderJpaRepository.findByUserIdAndStatus(userId, status, PageableSanitizer.capped(pageable))
                .map(bookingOrderEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingOrder> findByDepartureIdAndStatus(UUID departureId, BookingOrderStatus status) {
        return bookingOrderJpaRepository.findByDepartureIdAndStatus(departureId, status)
                .stream()
                .map(bookingOrderEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingOrder> findExpiredHolds(Instant holdExpiresAt) {
        return bookingOrderJpaRepository.findByStatusAndHoldExpiresAtBefore(BookingOrderStatus.HLD, holdExpiresAt)
                .stream()
                .map(bookingOrderEntityMapper::toDomain)
                .toList();
    }

    private BookingOrder toAggregate(BookingOrderJpaEntity orderEntity) {
        List<BookingOrderItemJpaEntity> itemEntities = bookingOrderItemJpaRepository
                .findByBookingOrderIdOrderByLineNoAsc(orderEntity.getBookingOrderId());
        return bookingOrderEntityMapper.toDomain(orderEntity, bookingOrderEntityMapper.toDomainItems(itemEntities));
    }

    private void syncItems(UUID bookingOrderId, BookingOrder bookingOrder) {
        List<BookingOrderItemJpaEntity> itemEntities = bookingOrder.getItems() == null
                ? Collections.emptyList()
                : bookingOrder.getItems().stream()
                .map(item -> bookingOrderItemEntityMapper.toEntity(item.toBuilder().bookingOrderId(bookingOrderId).version(null).build()))
                .toList();

        bookingOrderItemJpaRepository.deleteByBookingOrderId(bookingOrderId);
        if (!itemEntities.isEmpty()) {
            bookingOrderItemJpaRepository.saveAll(itemEntities);
        }
    }
}