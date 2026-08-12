package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.inventory.model.InventoryReservation;
import com.vetautet.app.domain.inventory.model.InventoryReservationStatus;
import com.vetautet.app.domain.inventory.repository.InventoryReservationRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.InventoryReservationJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.InventoryReservationEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class InventoryReservationRepositoryAdapter implements InventoryReservationRepository {

    private final InventoryReservationJpaRepository jpaRepository;
    private final InventoryReservationEntityMapper mapper;

    @Override
    public InventoryReservation save(InventoryReservation inventoryReservation) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(inventoryReservation)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryReservation> findById(UUID inventoryReservationId) {
        return jpaRepository.findById(inventoryReservationId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryReservation> findByBookingOrderIdAndStatus(UUID bookingOrderId, InventoryReservationStatus reservationStatus) {
        return jpaRepository.findByBookingOrderIdAndReservationStatus(bookingOrderId, reservationStatus)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryReservation> findByInventoryBucketId(UUID inventoryBucketId) {
        return jpaRepository.findByInventoryBucketId(inventoryBucketId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryReservation> findExpiredReservations(Instant holdExpiresAt) {
        return jpaRepository.findByReservationStatusAndHoldExpiresAtBefore(InventoryReservationStatus.HLD, holdExpiresAt)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}