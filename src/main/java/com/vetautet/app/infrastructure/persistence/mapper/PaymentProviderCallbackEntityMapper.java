
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.payment.model.PaymentProviderCallback;
import com.vetautet.app.infrastructure.persistence.jpa.entity.PaymentProviderCallbackJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderCallbackEntityMapper {

    public PaymentProviderCallback toDomain(PaymentProviderCallbackJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return PaymentProviderCallback.builder()
                .paymentCallbackId(entity.getPaymentCallbackId())
                .paymentTransactionId(entity.getPaymentTransactionId())
                .providerCode(entity.getProviderCode())
                .providerEventId(entity.getProviderEventId())
                .callbackStatus(entity.getCallbackStatus())
                .payload(entity.getPayload())
                .receivedAt(entity.getReceivedAt())
                .processedAt(entity.getProcessedAt())
                .errorMessage(entity.getErrorMessage())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public PaymentProviderCallbackJpaEntity toEntity(PaymentProviderCallback domain) {
        if (domain == null) {
            return null;
        }

        PaymentProviderCallbackJpaEntity entity = PaymentProviderCallbackJpaEntity.builder()
                .paymentCallbackId(domain.getPaymentCallbackId())
                .paymentTransactionId(domain.getPaymentTransactionId())
                .providerCode(domain.getProviderCode())
                .providerEventId(domain.getProviderEventId())
                .callbackStatus(domain.getCallbackStatus())
                .payload(domain.getPayload())
                .receivedAt(domain.getReceivedAt())
                .processedAt(domain.getProcessedAt())
                .errorMessage(domain.getErrorMessage())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }
}