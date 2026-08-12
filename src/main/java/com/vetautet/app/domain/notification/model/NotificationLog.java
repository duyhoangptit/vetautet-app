package com.vetautet.app.domain.notification.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class NotificationLog {

    private final UUID notificationLogId;
    private final String eventId;
    private final UUID referenceId;
    private final String referenceType;
    private final NotificationChannelCode channelCode;
    private final String templateCode;
    private final String recipient;
    private final List<String> toList;
    private final List<String> ccList;
    private final List<String> bccList;
    private final String subject;
    private final String content;
    private final Map<String, String> variables;
    private final NotificationStatus status;
    private final String providerCode;
    private final String providerMessageId;
    private final Instant sentAt;
    private final Instant deliveredAt;
    private final Instant failedAt;
    private final String errorCode;
    private final String errorMessage;
    private final Short retryCount;
    private final Short maxRetries;
    private final Instant nextRetryAt;

    private final Instant createdDate;
    private final Instant lastModifiedDate;
    private final String createdBy;
    private final String lastModifiedBy;
    private final Long version;

    public NotificationLog markSent(String providerCode, String providerMessageId) {
        return this.toBuilder()
                .status(NotificationStatus.SENT)
                .providerCode(providerCode)
                .providerMessageId(providerMessageId)
                .sentAt(Instant.now())
                .nextRetryAt(null)
                .build();
    }

    public NotificationLog markDelivered() {
        return this.toBuilder()
                .status(NotificationStatus.DELIVERED)
                .deliveredAt(Instant.now())
                .build();
    }

    public NotificationLog markFailed(String errorCode, String errorMessage, Instant nextRetryAt) {
        Short newRetryCount = (short) (this.retryCount + 1);
        boolean exhausted = newRetryCount >= this.maxRetries;
        return this.toBuilder()
                .status(NotificationStatus.FAILED)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .retryCount(newRetryCount)
                .failedAt(exhausted ? Instant.now() : null)
                .nextRetryAt(exhausted ? null : nextRetryAt)
                .build();
    }

    public NotificationLog markSkipped() {
        return this.toBuilder()
                .status(NotificationStatus.SKIPPED)
                .nextRetryAt(null)
                .build();
    }

    public boolean isRetryable() {
        return status == NotificationStatus.FAILED && retryCount < maxRetries;
    }
}