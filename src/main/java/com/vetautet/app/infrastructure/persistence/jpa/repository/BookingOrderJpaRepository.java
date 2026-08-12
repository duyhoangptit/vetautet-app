package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.domain.booking.model.BookingOrderStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.BookingOrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingOrderJpaRepository extends JpaRepository<BookingOrderJpaEntity, UUID> {

    Optional<BookingOrderJpaEntity> findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    Page<BookingOrderJpaEntity> findByUserId(UUID userId, Pageable pageable);

    Page<BookingOrderJpaEntity> findByUserIdAndStatus(UUID userId, BookingOrderStatus status, Pageable pageable);

    List<BookingOrderJpaEntity> findByDepartureIdAndStatus(UUID departureId, BookingOrderStatus status);

    List<BookingOrderJpaEntity> findByStatusAndHoldExpiresAtBefore(BookingOrderStatus status, Instant holdExpiresAt);
}