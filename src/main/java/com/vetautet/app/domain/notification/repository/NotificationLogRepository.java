package com.vetautet.app.domain.notification.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vetautet.app.domain.notification.model.NotificationLog;

public interface NotificationLogRepository {

    NotificationLog save(NotificationLog log);

    Optional<NotificationLog> findById(UUID notificationLogId);

    List<NotificationLog> findRetryable(Instant now, int limit);

    Optional<NotificationLog> findByEventId(String eventId);
}
 