package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.infrastructure.persistence.jpa.entity.BookingOrderItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingOrderItemJpaRepository extends JpaRepository<BookingOrderItemJpaEntity, UUID> {

    void deleteByBookingOrderId(UUID bookingOrderId);

    List<BookingOrderItemJpaEntity> findByBookingOrderIdOrderByLineNoAsc(UUID bookingOrderId);

    Optional<BookingOrderItemJpaEntity> findByBookingOrderIdAndLineNo(UUID bookingOrderId, Integer lineNo);

    List<BookingOrderItemJpaEntity> findByInventoryBucketId(UUID inventoryBucketId);
}