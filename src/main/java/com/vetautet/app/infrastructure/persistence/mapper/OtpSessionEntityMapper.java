
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.auth.model.OtpSession;
import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.domain.user.model.UserId;
import com.vetautet.app.infrastructure.persistence.jpa.entity.OtpSessionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OtpSessionEntityMapper {

    public OtpSession toDomain(OtpSessionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return OtpSession.builder()
                .otpSessionId(entity.getOtpSessionId())
                .userId(entity.getUserId() != null ? UserId.of(entity.getUserId()) : null)
                .email(Email.of(entity.getEmail()))
                .flowType(entity.getFlowType())
                .referenceToken(entity.getReferenceToken())
                .otpCode(entity.getOtpCode())
                .status(entity.getStatus())
                .attemptCount(entity.getAttemptCount())
                .maxAttempts(entity.getMaxAttempts())
                .expiresAt(entity.getExpiresAt())
                .verifiedAt(entity.getVerifiedAt())
                .data(entity.getData())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public OtpSessionJpaEntity toEntity(OtpSession domain) {
        if (domain == null) {
            return null;
        }

        OtpSessionJpaEntity entity = OtpSessionJpaEntity.builder()
                .otpSessionId(domain.getOtpSessionId())
                .userId(domain.getUserId() != null ? domain.getUserId().getValue() : null)
                .email(domain.getEmail().getValue())
                .flowType(domain.getFlowType())
                .referenceToken(domain.getReferenceToken())
                .otpCode(domain.getOtpCode())
                .status(domain.getStatus())
                .attemptCount(domain.getAttemptCount())
                .maxAttempts(domain.getMaxAttempts())
                .expiresAt(domain.getExpiresAt())
                .verifiedAt(domain.getVerifiedAt())
                .data(domain.getData())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }
}