package com.vetautet.app.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.vetautet.app.domain.notification.model.NotificationTemplate;
import com.vetautet.app.infrastructure.persistence.jpa.entity.NotificationTemplateJpaEntity;

@Component
public class NotificationTemplateEntityMapper {

    public NotificationTemplate toDomain(NotificationTemplateJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return NotificationTemplate.builder()
                .notificationTemplateId(entity.getNotificationTemplateId())
                .templateCode(entity.getTemplateCode())
                .channelCode(entity.getChannelCode())
                .locale(entity.getLocale())
                .templateName(entity.getTemplateName())
                .subjectTemplate(entity.getSubjectTemplate())
                .bodyTemplate(entity.getBodyTemplate())
                .variableKeys(entity.getVariableKeys())
                .toList(entity.getToList())
                .ccList(entity.getCcList())
                .bccList(entity.getBccList())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .version(entity.getVersion())
                .build();
    }

    public NotificationTemplateJpaEntity toEntity(NotificationTemplate domain) {
        if (domain == null) {
            return null;
        }
        NotificationTemplateJpaEntity entity = NotificationTemplateJpaEntity.builder()
                .notificationTemplateId(domain.getNotificationTemplateId())
                .templateCode(domain.getTemplateCode())
                .channelCode(domain.getChannelCode())
                .locale(domain.getLocale())
                .templateName(domain.getTemplateName())
                .subjectTemplate(domain.getSubjectTemplate())
                .bodyTemplate(domain.getBodyTemplate())
                .variableKeys(domain.getVariableKeys())
                .toList(domain.getToList())
                .ccList(domain.getCcList())
                .bccList(domain.getBccList())
                .description(domain.getDescription())
                .isActive(domain.getIsActive())
                .build();

        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
            entity.setCreatedDate(domain.getCreatedDate());
            entity.setCreatedBy(domain.getCreatedBy());
        }
        return entity;
    }
}
