
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.payment.model.PaymentTransaction;
import com.vetautet.app.infrastructure.persistence.jpa.entity.PaymentTransactionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentTransactionEntityMapper {

    public PaymentTransaction toDomain(PaymentTransactionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return PaymentTransaction.builder()
                .paymentTransactionId(entity.getPaymentTransactionId())
                .bookingOrderId(entity.getBookingOrderId())
                .paymentMethodCode(entity.getPaymentMethodCode())
                .providerCode(entity.getProviderCode())
                .providerTransactionId(entity.getProviderTransactionId())
                .providerPaymentUrl(entity.getProviderPaymentUrl())
                .amount(entity.getAmount())
                .currencyCode(entity.getCurrencyCode())
                .status(entity.getStatus())
                .requestedAt(entity.getRequestedAt())
                .authorizedAt(entity.getAuthorizedAt())
                .settledAt(entity.getSettledAt())
                .expiresAt(entity.getExpiresAt())
                .failureCode(entity.getFailureCode())
                .failureMessage(entity.getFailureMessage())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public PaymentTransactionJpaEntity toEntity(PaymentTransaction domain) {
        if (domain == null) {
            return null;
        }

        PaymentTransactionJpaEntity entity = PaymentTransactionJpaEntity.builder()
                .paymentTransactionId(domain.getPaymentTransactionId())
                .bookingOrderId(domain.getBookingOrderId())
                .paymentMethodCode(domain.getPaymentMethodCode())
                .providerCode(domain.getProviderCode())
                .providerTransactionId(domain.getProviderTransactionId())
                .providerPaymentUrl(domain.getProviderPaymentUrl())
                .amount(domain.getAmount())
                .currencyCode(domain.getCurrencyCode())
                .status(domain.getStatus())
                .requestedAt(domain.getRequestedAt())
                .authorizedAt(domain.getAuthorizedAt())
                .settledAt(domain.getSettledAt())
                .expiresAt(domain.getExpiresAt())
                .failureCode(domain.getFailureCode())
                .failureMessage(domain.getFailureMessage())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }
}