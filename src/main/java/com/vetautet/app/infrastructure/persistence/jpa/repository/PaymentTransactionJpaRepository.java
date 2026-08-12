package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.domain.payment.model.PaymentTransactionStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.PaymentTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionJpaRepository extends JpaRepository<PaymentTransactionJpaEntity, UUID> {

    List<PaymentTransactionJpaEntity> findByBookingOrderIdOrderByRequestedAtDesc(UUID bookingOrderId);

    Optional<PaymentTransactionJpaEntity> findByProviderCodeAndProviderTransactionId(String providerCode, String providerTransactionId);

    List<PaymentTransactionJpaEntity> findByStatusAndExpiresAtBefore(PaymentTransactionStatus status, Instant expiresAt);
}