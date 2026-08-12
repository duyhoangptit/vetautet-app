package com.vetautet.app.infrastructure.persistence.mapper;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.app.domain.notification.model.NotificationLog;
import com.vetautet.app.infrastructure.persistence.jpa.entity.NotificationLogJpaEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationLogEntityMapper {

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public NotificationLog toDomain(NotificationLogJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return NotificationLog.builder()
                .notificationLogId(entity.getNotificationLogId())
                .eventId(entity.getEventId())
                .referenceId(entity.getReferenceId())
                .referenceType(entity.getReferenceType())
                .channelCode(entity.getChannelCode())
                .templateCode(entity.getTemplateCode())
                .recipient(entity.getRecipient())
                .toList(entity.getToList())
                .ccList(entity.getCcList())
                .bccList(entity.getBccList())
                .subject(entity.getSubject())
                .content(entity.getContent())
                .variables(deserializeVariables(entity.getVariables()))
                .status(entity.getStatus())
                .providerCode(entity.getProviderCode())
                .providerMessageId(entity.getProviderMessageId())
                .sentAt(entity.getSentAt())
                .deliveredAt(entity.getDeliveredAt())
                .failedAt(entity.getFailedAt())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .retryCount(entity.getRetryCount())
                .maxRetries(entity.getMaxRetries())
                .nextRetryAt(entity.getNextRetryAt())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public NotificationLogJpaEntity toEntity(NotificationLog domain) {
        if (domain == null) {
            return null;
        }
        NotificationLogJpaEntity entity = NotificationLogJpaEntity.builder()
                .notificationLogId(domain.getNotificationLogId())
                .eventId(domain.getEventId())
                .referenceId(domain.getReferenceId())
                .referenceType(domain.getReferenceType())
                .channelCode(domain.getChannelCode())
                .templateCode(domain.getTemplateCode())
                .recipient(resolveRecipient(domain))
                .toList(domain.getToList())
                .ccList(domain.getCcList())
                .bccList(domain.getBccList())
                .subject(domain.getSubject())
                .content(domain.getContent())
                .variables(serializeVariables(domain.getVariables()))
                .status(domain.getStatus())
                .providerCode(domain.getProviderCode())
                .providerMessageId(domain.getProviderMessageId())
                .sentAt(domain.getSentAt())
                .deliveredAt(domain.getDeliveredAt())
                .failedAt(domain.getFailedAt())
                .errorCode(domain.getErrorCode())
                .errorMessage(domain.getErrorMessage())
                .retryCount(domain.getRetryCount())
                .maxRetries(domain.getMaxRetries())
                .nextRetryAt(domain.getNextRetryAt())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }
        return entity;
    }

    private String serializeVariables(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize notification variables", e);
            return null;
        }
    }

    private Map<String, String> deserializeVariables(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize notification variables from JSON: {}", json, e);
            return null;
        }
    }

    /**
     * For EMAIL channel: use first address from toList as recipient when no explicit recipient is set.
     * For other channels: recipient is the direct address (phone, token, chat_id).
     */
    private String resolveRecipient(NotificationLog domain) {
        if (domain.getRecipient() != null) {
            return domain.getRecipient();
        }
        if (domain.getToList() != null && !domain.getToList().isEmpty()) {
            return domain.getToList().getFirst();
        }
        return "unknown";
    }
}
 