
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.auth.model.KeyId;
import com.vetautet.app.domain.auth.model.PublicKey;
import com.vetautet.app.domain.auth.model.RsaKeyPair;
import com.vetautet.app.domain.user.model.UserId;
import com.vetautet.app.infrastructure.persistence.jpa.entity.RsaKeyPairJpaEntity;
import org.springframework.stereotype.Component;

/**
* Mapper between Domain RsaKeyPair model and JPA RsaKeyPairEntity
* Infrastructure layer - responsible for converting between domain and
* persistence models
*/
@Component
public class RsaKeyPairEntityMapper {

    /**
     * Convert JPA entity to Domain model
     */
    public RsaKeyPair toDomain(RsaKeyPairJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return RsaKeyPair.builder()
                .keyId(KeyId.of(entity.getKeyId()))
                .userId(UserId.of(entity.getUserId()))
                .publicKey(PublicKey.of(entity.getPublicKey()))
                .algorithm(entity.getAlgorithm())
                .keySize(entity.getKeySize())
                .isActive(entity.getIsActive())
                .expiresAt(entity.getExpiresAt())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    /**
     * Convert Domain model to JPA entity
     */
    public RsaKeyPairJpaEntity toEntity(RsaKeyPair domain) {
        if (domain == null) {
            return null;
        }

        RsaKeyPairJpaEntity entity = RsaKeyPairJpaEntity.builder()
                .keyId(domain.getKeyId().getValue())
                .userId(domain.getUserId().getValue())
                .publicKey(domain.getPublicKey().getPemFormat())
                .algorithm(domain.getAlgorithm())
                .keySize(domain.getKeySize())
                .isActive(domain.getIsActive())
                .expiresAt(domain.getExpiresAt())
                .build();

        // Set audit fields if updating existing entity
        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }

        return entity;
    }

    /**
     * Update existing JPA entity with Domain model data
     */
    public void updateEntity(RsaKeyPairJpaEntity entity, RsaKeyPair domain) {
        if (entity == null || domain == null) {
            return;
        }

        entity.setPublicKey(domain.getPublicKey().getPemFormat());
        entity.setAlgorithm(domain.getAlgorithm());
        entity.setKeySize(domain.getKeySize());
        entity.setIsActive(domain.getIsActive());
        entity.setExpiresAt(domain.getExpiresAt());
    }
}
