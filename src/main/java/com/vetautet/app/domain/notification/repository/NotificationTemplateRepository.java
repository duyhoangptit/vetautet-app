package com.vetautet.app.domain.notification.repository;

import java.util.Optional;

import com.vetautet.app.domain.notification.model.NotificationChannelCode;
import com.vetautet.app.domain.notification.model.NotificationTemplate;

public interface NotificationTemplateRepository {

    Optional<NotificationTemplate> findActiveByCodeAndChannelAndLocale(
            String templateCode,
            NotificationChannelCode channelCode,
            String locale);

    NotificationTemplate save(NotificationTemplate template);
}
 