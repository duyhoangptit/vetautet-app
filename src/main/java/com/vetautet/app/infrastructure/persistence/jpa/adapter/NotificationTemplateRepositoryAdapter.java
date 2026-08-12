package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vetautet.app.domain.notification.model.NotificationChannelCode;
import com.vetautet.app.domain.notification.model.NotificationTemplate;
import com.vetautet.app.domain.notification.repository.NotificationTemplateRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.NotificationTemplateJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.NotificationTemplateEntityMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class NotificationTemplateRepositoryAdapter implements NotificationTemplateRepository {

    private final NotificationTemplateJpaRepository jpaRepository;
    private final NotificationTemplateEntityMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationTemplate> findActiveByCodeAndChannelAndLocale(
            String templateCode,
            NotificationChannelCode channelCode,
            String locale) {
        return jpaRepository
                .findByTemplateCodeAndChannelCodeAndLocaleAndIsActiveTrue(templateCode, channelCode, locale)
                .map(mapper::toDomain);
    }

    @Override
    public NotificationTemplate save(NotificationTemplate template) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(template)));
    }
}
 