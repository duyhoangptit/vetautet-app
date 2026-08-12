
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.payment.model.PaymentMethodCode;
import com.vetautet.app.domain.payment.model.PaymentTransactionStatus;
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
@Table(name = "payment_transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_provider_transaction", columnNames = {"provider_code", "provider_transaction_id"})
}, indexes = {
        @Index(name = "idx_payment_transactions_order_status", columnList = "booking_order_id,status,requested_at"),
        @Index(name = "idx_payment_transactions_provider_status", columnList = "provider_code,status,expires_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "payment_transaction_id", nullable = false, updatable = false)
    private UUID paymentTransactionId;

    @Column(name = "booking_order_id", nullable = false)
    private UUID bookingOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_code", nullable = false, length = 20)
    private PaymentMethodCode paymentMethodCode;

    @Column(name = "provider_code", nullable = false, length = 30)
    private String providerCode;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(name = "provider_payment_url", length = 1000)
    private String providerPaymentUrl;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 3)
    private PaymentTransactionStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;
}
