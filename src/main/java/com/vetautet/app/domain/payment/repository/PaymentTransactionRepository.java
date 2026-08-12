package com.vetautet.app.domain.payment.repository;

import com.vetautet.app.domain.payment.model.PaymentTransaction;
import com.vetautet.app.domain.payment.model.PaymentTransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository {

    PaymentTransaction save(PaymentTransaction paymentTransaction);

    Optional<PaymentTransaction> findById(UUID paymentTransactionId);

    List<PaymentTransaction> findByBookingOrderId(UUID bookingOrderId);

    Optional<PaymentTransaction> findByProviderCodeAndProviderTransactionId(String providerCode, String providerTransactionId);

    List<PaymentTransaction> findExpiredTransactions(PaymentTransactionStatus status, Instant expiresAt);
}