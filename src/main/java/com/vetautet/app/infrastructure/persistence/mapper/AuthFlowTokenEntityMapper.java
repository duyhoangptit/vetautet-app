package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.auth.model.AuthFlowToken;
import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.domain.user.model.UserId;
import com.vetautet.app.infrastructure.persistence.jpa.entity.AuthFlowTokenJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthFlowTokenEntityMapper {

    public AuthFlowToken toDomain(AuthFlowTokenJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return AuthFlowToken.builder()
                .authFlowTokenId(entity.getAuthFlowTokenId())
                .userId(entity.getUserId() != null ? UserId.of(entity.getUserId()) : null)
                .email(Email.of(entity.getEmail()))
                .flowType(entity.getFlowType())
                .token(entity.getToken())
                .expiresAt(entity.getExpiresAt())
                .consumedAt(entity.getConsumedAt())
                .status(entity.getStatus())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public AuthFlowTokenJpaEntity toEntity(AuthFlowToken domain) {
        if (domain == null) {
            return null;
        }

        AuthFlowTokenJpaEntity entity = AuthFlowTokenJpaEntity.builder()
                .authFlowTokenId(domain.getAuthFlowTokenId())
                .userId(domain.getUserId() != null ? domain.getUserId().getValue() : null)
                .email(domain.getEmail().getValue())
                .flowType(domain.getFlowType())
                .token(domain.getToken())
                .expiresAt(domain.getExpiresAt())
                .consumedAt(domain.getConsumedAt())
                .status(domain.getStatus())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }
}