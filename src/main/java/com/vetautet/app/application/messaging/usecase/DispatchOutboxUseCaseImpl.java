package com.vetautet.app.application.messaging.usecase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.app.application.messaging.dto.DispatchOutboxCommand;
import com.vetautet.app.application.messaging.dto.DispatchOutboxResultDto;
import com.vetautet.app.application.messaging.dto.DispatchedOutboxEventDto;
import com.vetautet.app.application.messaging.port.input.DispatchOutboxUseCase;
import com.vetautet.app.application.messaging.port.output.OutboxPublisher;
import com.vetautet.app.domain.messaging.model.OutboxEvent;
import com.vetautet.app.domain.messaging.model.OutboxPublishStatus;
import com.vetautet.app.domain.messaging.repository.OutboxEventRepository;
import com.vetautet.app.shared.common.context.RequestIdContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchOutboxUseCaseImpl implements DispatchOutboxUseCase {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final List<OutboxPublishStatus> DISPATCHABLE_STATUSES =
            List.of(OutboxPublishStatus.NEW, OutboxPublishStatus.FAI);
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.dispatch.max-retries:5}")
    private int maxRetries;

    @Value("${app.outbox.dispatch.retry-backoff-seconds:60}")
    private long retryBackoffSeconds;

    @Override
    @Transactional
    public DispatchOutboxResultDto execute(DispatchOutboxCommand command) {
        Instant dispatchTime = command.getDispatchTime() != null ? command.getDispatchTime() : Instant.now();
        Integer requestedBatchSize = command.getBatchSize();
        int batchSize = requestedBatchSize == null || requestedBatchSize <= 0
                ? DEFAULT_BATCH_SIZE
                : requestedBatchSize;

        List<OutboxEvent> pendingEvents = outboxEventRepository.findDispatchable(DISPATCHABLE_STATUSES, dispatchTime)
                .stream()
                .limit(batchSize)
                .toList();

        List<DispatchedOutboxEventDto> eventResults = new ArrayList<>();
        int[] counts = new int[3]; // [published, failed, dlq]

        for (OutboxEvent event : pendingEvents) {
            processEvent(event, dispatchTime, eventResults, counts);
        }

        return DispatchOutboxResultDto.builder()
                .requestedBatchSize(batchSize)
                .processedCount(pendingEvents.size())
                .publishedCount(counts[0])
                .failedCount(counts[1])
                .dlqCount(counts[2])
                .events(eventResults)
                .build();
    }

    /** counts[0]=published, counts[1]=failed, counts[2]=dlq */
    private void processEvent(OutboxEvent event, Instant dispatchTime,
                              List<DispatchedOutboxEventDto> results, int[] counts) {
        try {
            publishWithRequestContext(event);
            OutboxEvent publishedEvent = outboxEventRepository.save(event.toBuilder()
                    .publishStatus(OutboxPublishStatus.PUB)
                    .publishedAt(dispatchTime)
                    .nextAttemptAt(null)
                    .build());
            counts[0]++;
            results.add(toDto(publishedEvent, null));
        } catch (RuntimeException ex) {
            results.add(toDto(handlePublishFailure(event, dispatchTime, counts), ex.getMessage()));
        }
    }

    private OutboxEvent handlePublishFailure(OutboxEvent event, Instant dispatchTime, int[] counts) {
        int newRetryCount = event.getRetryCount() + 1;
        boolean exhausted = newRetryCount >= maxRetries;
        OutboxPublishStatus nextStatus = exhausted ? OutboxPublishStatus.DLQ : OutboxPublishStatus.FAI;

        if (exhausted) {
            log.warn("Outbox event moved to DLQ after {} retries: eventId={} eventType={}",
                    newRetryCount, event.getOutboxEventId(), event.getEventType());
            counts[2]++;
        } else {
            counts[1]++;
        }

        return outboxEventRepository.save(event.toBuilder()
                .publishStatus(nextStatus)
                .retryCount(newRetryCount)
                .nextAttemptAt(exhausted ? null : dispatchTime.plusSeconds(retryBackoffSeconds))
                .build());
    }

    private void publishWithRequestContext(OutboxEvent event) {
        RequestIdContext.runWithRequestId(extractRequestId(event.getHeaders()), () -> outboxPublisher.publish(event));
    }

    private String extractRequestId(String serializedHeaders) {
        if (serializedHeaders == null || serializedHeaders.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> headers = objectMapper.readValue(serializedHeaders, MAP_TYPE_REFERENCE);
            return RequestIdContext.readRequestId(headers);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize outbox headers", ex);
        }
    }

    private DispatchedOutboxEventDto toDto(OutboxEvent outboxEvent, String errorMessage) {
        return DispatchedOutboxEventDto.builder()
                .outboxEventId(outboxEvent.getOutboxEventId())
                .aggregateType(outboxEvent.getAggregateType())
                .aggregateId(outboxEvent.getAggregateId())
                .eventType(outboxEvent.getEventType())
                .publishStatus(outboxEvent.getPublishStatus())
                .publishedAt(outboxEvent.getPublishedAt())
                .retryCount(outboxEvent.getRetryCount())
                .errorMessage(errorMessage)
                .build();
    }
}
 