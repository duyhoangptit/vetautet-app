
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.vetautet.app.domain.notification.model.NotificationChannelCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "notification_templates",
        indexes = {
                @Index(name = "idx_notification_templates_lookup",
                        columnList = "template_code, channel_code, locale")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "notification_template_id", nullable = false, updatable = false)
    private UUID notificationTemplateId;

    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_code", nullable = false, length = 50)
    private NotificationChannelCode channelCode;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "template_name", nullable = false, length = 255)
    private String templateName;

    @Column(name = "subject_template", columnDefinition = "text")
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "text")
    private String bodyTemplate;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "variable_keys", columnDefinition = "text[]")
    private List<String> variableKeys;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "to_list", columnDefinition = "text[]")
    private List<String> toList;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "cc_list", columnDefinition = "text[]")
    private List<String> ccList;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "bcc_list", columnDefinition = "text[]")
    private List<String> bccList;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}

