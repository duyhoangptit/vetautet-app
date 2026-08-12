
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.vetautet.app.domain.notification.model.NotificationChannelCode;
import com.vetautet.app.domain.notification.model.NotificationStatus;

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
        name = "notification_logs",
        indexes = {
                @Index(name = "idx_notification_logs_retry",
                        columnList = "status, next_retry_at"),
                @Index(name = "idx_notification_logs_reference",
                        columnList = "reference_id, reference_type"),
                @Index(name = "idx_notification_logs_event_id",
                        columnList = "event_id"),
                @Index(name = "idx_notification_logs_created_at",
                        columnList = "created_date")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "notification_log_id", nullable = false, updatable = false)
    private UUID notificationLogId;

    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reference_type", length = 100)
    private String referenceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_code", nullable = false, length = 50)
    private NotificationChannelCode channelCode;

    @Column(name = "template_code", length = 100)
    private String templateCode;

    @Column(name = "recipient", nullable = false, length = 500)
    private String recipient;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "to_list", columnDefinition = "text[]")
    private List<String> toList;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "cc_list", columnDefinition = "text[]")
    private List<String> ccList;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "bcc_list", columnDefinition = "text[]")
    private List<String> bccList;

    @Column(name = "subject", columnDefinition = "text")
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables", columnDefinition = "jsonb")
    private String variables;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "provider_code", length = 50)
    private String providerCode;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Short retryCount;

    @Column(name = "max_retries", nullable = false)
    private Short maxRetries;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
}