package com.vetautet.app.application.notification.usecase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.app.shared.common.util.JsonUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vetautet.app.application.notification.dto.NotificationResultDto;
import com.vetautet.app.application.notification.dto.SendNotificationCommand;
import com.vetautet.app.application.notification.port.input.SendNotificationUseCase;
import com.vetautet.app.domain.messaging.model.OutboxEvent;
import com.vetautet.app.domain.messaging.model.OutboxPublishStatus;
import com.vetautet.app.domain.messaging.repository.OutboxEventRepository;
import com.vetautet.app.domain.notification.model.NotificationLog;
import com.vetautet.app.domain.notification.model.NotificationStatus;
import com.vetautet.app.domain.notification.model.NotificationTemplate;
import com.vetautet.app.domain.notification.repository.NotificationLogRepository;
import com.vetautet.app.domain.notification.repository.NotificationTemplateRepository;
import com.vetautet.app.infrastructure.notification.TemplateRenderer;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SendNotificationUseCaseImpl implements SendNotificationUseCase {

    private static final String AGGREGATE_TYPE = "NOTIFICATION";
    private static final String EVENT_TYPE = "notification-email-requested";

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TemplateRenderer templateRenderer;
    private final ObjectMapper objectMapper;

    @Override
    public NotificationResultDto execute(SendNotificationCommand command) {
        log.debug("Sending notification: channel={}, template={}, recipient={}",
                command.getChannelCode(), command.getTemplateCode(), command.getRecipient());

        String resolvedSubject = command.getSubject();
        String resolvedContent = command.getContent();
        List<String> resolvedToList = merge(null, command.getToList());
        List<String> resolvedCcList = merge(null, command.getCcList());
        List<String> resolvedBccList = merge(null, command.getBccList());

        if (command.getTemplateCode() != null) {
            String locale = command.getLocale() != null ? command.getLocale() : "vi";
            NotificationTemplate template = templateRepository
                    .findActiveByCodeAndChannelAndLocale(command.getTemplateCode(), command.getChannelCode(), locale)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND,
                            command.getTemplateCode() + "/" + command.getChannelCode() + "/" + locale));

            resolvedSubject = templateRenderer.render(template.getSubjectTemplate(), command.getVariables());
            resolvedContent = templateRenderer.render(template.getBodyTemplate(), command.getVariables());
            resolvedToList = merge(template.getToList(), command.getToList());
            resolvedCcList = merge(template.getCcList(), command.getCcList());
            resolvedBccList = merge(template.getBccList(), command.getBccList());
        }

        UUID logId = UUID.randomUUID();
        NotificationLog notificationLog = NotificationLog.builder()
                .notificationLogId(logId)
                .eventId(command.getEventId())
                .referenceId(command.getReferenceId())
                .referenceType(command.getReferenceType())
                .channelCode(command.getChannelCode())
                .templateCode(command.getTemplateCode())
                .recipient(command.getRecipient())
                .toList(resolvedToList)
                .ccList(resolvedCcList)
                .bccList(resolvedBccList)
                .subject(resolvedSubject)
                .content(resolvedContent)
                .variables(command.getVariables())
                .status(NotificationStatus.PENDING)
                .retryCount((short) 0)
                .maxRetries(command.getMaxRetries())
                .nextRetryAt(Instant.now())
                .build();

        NotificationLog savedLog = logRepository.save(notificationLog);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .outboxEventId(UUID.randomUUID())
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(savedLog.getNotificationLogId())
                .eventType(EVENT_TYPE)
                .partitionKey(savedLog.getNotificationLogId().toString())
                .payload(JsonUtil.writeJson(objectMapper, notificationLog, "Failed to serialize notification log for outbox event"))
                .publishStatus(OutboxPublishStatus.NEW)
                .retryCount(0)
                .nextAttemptAt(Instant.now())
                .build();

        outboxEventRepository.save(outboxEvent);

        log.info("Notification log {} created with status PENDING for channel {}",
                savedLog.getNotificationLogId(), command.getChannelCode());

        return NotificationResultDto.builder()
                .notificationLogId(savedLog.getNotificationLogId())
                .status(savedLog.getStatus())
                .build();
    }

    /**
     * Merges two recipient lists (template-defined + caller-supplied) into a
     * de-duplicated list preserving insertion order.
     */
    private List<String> merge(List<String> templateList, List<String> requestList) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (templateList != null) {
            merged.addAll(templateList);
        }
        if (requestList != null) {
            merged.addAll(requestList);
        }
        return merged.isEmpty() ? null : new ArrayList<>(merged);
    }
}
 