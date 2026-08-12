package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.payment.model.PaymentCallbackStatus;
import com.vetautet.app.domain.payment.model.PaymentProviderCallback;
import com.vetautet.app.domain.payment.repository.PaymentProviderCallbackRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.PaymentProviderCallbackJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.PaymentProviderCallbackEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class PaymentProviderCallbackRepositoryAdapter implements PaymentProviderCallbackRepository {

    private final PaymentProviderCallbackJpaRepository jpaRepository;
    private final PaymentProviderCallbackEntityMapper mapper;

    @Override
    public PaymentProviderCallback save(PaymentProviderCallback paymentProviderCallback) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(paymentProviderCallback)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentProviderCallback> findById(UUID paymentCallbackId) {
        return jpaRepository.findById(paymentCallbackId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentProviderCallback> findByProviderCodeAndProviderEventId(String providerCode, String providerEventId) {
        return jpaRepository.findByProviderCodeAndProviderEventId(providerCode, providerEventId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentProviderCallback> findByPaymentTransactionId(UUID paymentTransactionId) {
        return jpaRepository.findByPaymentTransactionIdOrderByReceivedAtDesc(paymentTransactionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentProviderCallback> findByStatus(PaymentCallbackStatus callbackStatus) {
        return jpaRepository.findByCallbackStatus(callbackStatus)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}