
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.payment.model.PaymentCallbackStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_provider_callbacks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_provider_event", columnNames = {"provider_code", "provider_event_id"})
}, indexes = {
        @Index(name = "idx_payment_callbacks_transaction", columnList = "payment_transaction_id,received_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProviderCallbackJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "payment_callback_id", nullable = false, updatable = false)
    private UUID paymentCallbackId;

    @Column(name = "payment_transaction_id", nullable = false)
    private UUID paymentTransactionId;

    @Column(name = "provider_code", nullable = false, length = 30)
    private String providerCode;

    @Column(name = "provider_event_id", nullable = false, length = 100)
    private String providerEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "callback_status", nullable = false, length = 3)
    private PaymentCallbackStatus callbackStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;
}