package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.domain.payment.model.PaymentCallbackStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.PaymentProviderCallbackJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentProviderCallbackJpaRepository extends JpaRepository<PaymentProviderCallbackJpaEntity, UUID> {

    Optional<PaymentProviderCallbackJpaEntity> findByProviderCodeAndProviderEventId(String providerCode, String providerEventId);

    List<PaymentProviderCallbackJpaEntity> findByPaymentTransactionIdOrderByReceivedAtDesc(UUID paymentTransactionId);

    List<PaymentProviderCallbackJpaEntity> findByCallbackStatus(PaymentCallbackStatus callbackStatus);
}