
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.booking.model.BookingOrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_orders", indexes = {
        @Index(name = "uk_booking_orders_code", columnList = "order_code", unique = true),
        @Index(name = "idx_booking_orders_user_status", columnList = "user_id,status,created_date"),
        @Index(name = "idx_booking_orders_departure_status", columnList = "departure_id,status,hold_expires_at"),
        @Index(name = "idx_booking_orders_hold_expires_at", columnList = "hold_expires_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BookingOrderJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "booking_order_id", nullable = false, updatable = false)
    private UUID bookingOrderId;

    @Column(name = "order_code", nullable = false, unique = true, length = 40)
    private String orderCode;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "departure_id", nullable = false)
    private UUID departureId;

    @Column(name = "booking_channel", nullable = false, length = 20)
    private String bookingChannel;

    @Column(name = "customer_full_name", nullable = false, length = 150)
    private String customerFullName;

    @Column(name = "customer_email", nullable = false, length = 100)
    private String customerEmail;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 3)
    private BookingOrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "hold_expires_at", nullable = false)
    private Instant holdExpiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}