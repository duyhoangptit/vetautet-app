package com.vetautet.app.infrastructure.persistence.jpa.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vetautet.app.domain.notification.model.NotificationChannelCode;
import com.vetautet.app.infrastructure.persistence.jpa.entity.NotificationTemplateJpaEntity;

@Repository
public interface NotificationTemplateJpaRepository extends JpaRepository<NotificationTemplateJpaEntity, UUID> {

    Optional<NotificationTemplateJpaEntity> findByTemplateCodeAndChannelCodeAndLocaleAndIsActiveTrue(
            String templateCode,
            NotificationChannelCode channelCode,
            String locale);
}
 