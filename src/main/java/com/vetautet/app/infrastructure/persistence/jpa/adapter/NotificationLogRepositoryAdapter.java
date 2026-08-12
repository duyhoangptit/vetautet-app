package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vetautet.app.domain.notification.model.NotificationLog;
import com.vetautet.app.domain.notification.repository.NotificationLogRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.NotificationLogJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.NotificationLogEntityMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class NotificationLogRepositoryAdapter implements NotificationLogRepository {

    private final NotificationLogJpaRepository jpaRepository;
    private final NotificationLogEntityMapper mapper;

    @Override
    public NotificationLog save(NotificationLog log) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(log)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationLog> findById(UUID notificationLogId) {
        return jpaRepository.findById(notificationLogId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLog> findRetryable(Instant now, int limit) {
        return jpaRepository.findRetryable(now, PageRequest.of(0, limit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationLog> findByEventId(String eventId) {
        return jpaRepository.findByEventId(eventId).map(mapper::toDomain);
    }
}
 