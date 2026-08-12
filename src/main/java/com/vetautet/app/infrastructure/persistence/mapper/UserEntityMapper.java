
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.user.model.*;
import com.vetautet.app.infrastructure.persistence.jpa.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

/**
* Mapper between Domain User model and JPA UserEntity
* Infrastructure layer - responsible for converting between domain and
* persistence models
*/
@Component
public class UserEntityMapper {

    /**
     * Convert JPA entity to Domain model
     */
    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return User.builder()
                .userId(UserId.of(entity.getUserId()))
                .username(entity.getUsername())
                .personalInfo(PersonalInfo.builder()
                        .firstName(entity.getPiiFirstName())
                        .lastName(entity.getPiiLastName())
                        .mobileCountryCode(entity.getMobileCountryCode())
                        .mobileNumber(entity.getMobileNumber())
                        .avatarUrl(entity.getAvatarUrl())
                        .build())
                .email(Email.of(entity.getEmail()))
                .status(UserStatus.fromCode(entity.getStatus()))
                .isOnboarding(entity.getIsOnboarding())
                .passwordHash(entity.getPasswordHash())
                .failedLoginAttempts(entity.getFailedLoginAttempts())
                .lockedUntil(entity.getLockedUntil())
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
    public UserJpaEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        UserJpaEntity entity = UserJpaEntity.builder()
                .userId(domain.getUserId() != null ? domain.getUserId().getValue() : null)
                .username(domain.getUsername())
                .piiFirstName(domain.getPersonalInfo().getFirstName())
                .piiLastName(domain.getPersonalInfo().getLastName())
                .email(domain.getEmail().getValue())
                .mobileCountryCode(domain.getPersonalInfo().getMobileCountryCode())
                .mobileNumber(domain.getPersonalInfo().getMobileNumber())
                .avatarUrl(domain.getPersonalInfo().getAvatarUrl())
                .status(domain.getStatus().getCode())
                .isOnboarding(domain.getIsOnboarding())
                .passwordHash(domain.getPasswordHash())
                .failedLoginAttempts(domain.getFailedLoginAttempts())
                .lockedUntil(domain.getLockedUntil())
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
    public void updateEntity(UserJpaEntity entity, User domain) {
        if (entity == null || domain == null) {
            return;
        }

        entity.setUsername(domain.getUsername());
        entity.setPiiFirstName(domain.getPersonalInfo().getFirstName());
        entity.setPiiLastName(domain.getPersonalInfo().getLastName());
        entity.setEmail(domain.getEmail().getValue());
        entity.setMobileCountryCode(domain.getPersonalInfo().getMobileCountryCode());
        entity.setMobileNumber(domain.getPersonalInfo().getMobileNumber());
        entity.setAvatarUrl(domain.getPersonalInfo().getAvatarUrl());
        entity.setStatus(domain.getStatus().getCode());
        entity.setIsOnboarding(domain.getIsOnboarding());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setFailedLoginAttempts(domain.getFailedLoginAttempts());
        entity.setLockedUntil(domain.getLockedUntil());
    }
}
 