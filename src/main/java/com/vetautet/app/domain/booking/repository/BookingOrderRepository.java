package com.vetautet.app.domain.booking.repository;

import com.vetautet.app.domain.booking.model.BookingOrder;
import com.vetautet.app.domain.booking.model.BookingOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingOrderRepository {

    BookingOrder save(BookingOrder bookingOrder);

    Optional<BookingOrder> findById(UUID bookingOrderId);

    Optional<BookingOrder> findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    Page<BookingOrder> findByUserId(UUID userId, Pageable pageable);

    Page<BookingOrder> findByUserIdAndStatus(UUID userId, BookingOrderStatus status, Pageable pageable);

    List<BookingOrder> findByDepartureIdAndStatus(UUID departureId, BookingOrderStatus status);

    List<BookingOrder> findExpiredHolds(Instant holdExpiresAt);
}