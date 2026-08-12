package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.payment.model.PaymentTransaction;
import com.vetautet.app.domain.payment.model.PaymentTransactionStatus;
import com.vetautet.app.domain.payment.repository.PaymentTransactionRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.PaymentTransactionJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.PaymentTransactionEntityMapper;
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
public class PaymentTransactionRepositoryAdapter implements PaymentTransactionRepository {

    private final PaymentTransactionJpaRepository jpaRepository;
    private final PaymentTransactionEntityMapper mapper;

    @Override
    public PaymentTransaction save(PaymentTransaction paymentTransaction) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(paymentTransaction)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentTransaction> findById(UUID paymentTransactionId) {
        return jpaRepository.findById(paymentTransactionId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransaction> findByBookingOrderId(UUID bookingOrderId) {
        return jpaRepository.findByBookingOrderIdOrderByRequestedAtDesc(bookingOrderId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentTransaction> findByProviderCodeAndProviderTransactionId(String providerCode, String providerTransactionId) {
        return jpaRepository.findByProviderCodeAndProviderTransactionId(providerCode, providerTransactionId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransaction> findExpiredTransactions(PaymentTransactionStatus status, Instant expiresAt) {
        return jpaRepository.findByStatusAndExpiresAtBefore(status, expiresAt)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}