package com.vetautet.app.domain.payment.repository;

import com.vetautet.app.domain.payment.model.PaymentCallbackStatus;
import com.vetautet.app.domain.payment.model.PaymentProviderCallback;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentProviderCallbackRepository {

    PaymentProviderCallback save(PaymentProviderCallback paymentProviderCallback);

    Optional<PaymentProviderCallback> findById(UUID paymentCallbackId);

    Optional<PaymentProviderCallback> findByProviderCodeAndProviderEventId(String providerCode, String providerEventId);

    List<PaymentProviderCallback> findByPaymentTransactionId(UUID paymentTransactionId);

    List<PaymentProviderCallback> findByStatus(PaymentCallbackStatus callbackStatus);
}