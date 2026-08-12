package com.vetautet.app.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.app.application.messaging.port.output.OutboxPublisher;
import com.vetautet.app.domain.messaging.model.OutboxEvent;
import com.vetautet.app.shared.common.context.RequestIdContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaOutboxPublisherAdapter implements OutboxPublisher {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final Map<String, String> TOPIC_NOTIFICATION_MAP = new HashMap<>(
            Map.of(
                    "notification-email-requested", "notification.email.requested.v1",
                    "notification-sms-requested", "notification.sms.requested.v1",
                    "notification-notify-requested", "notification.notify.requested.v1"
            )
    );

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

//    @Value("${app.kafka.outbox.topic}")
//    private String outboxTopic;
//
//    @Value("${notification-email-topic}")
//    private String outboxNotification;

    @Value("${app.kafka.outbox.send-timeout:10s}")
    private Duration sendTimeout;

    @Override
    public void publish(OutboxEvent outboxEvent) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                TOPIC_NOTIFICATION_MAP.get(outboxEvent.getEventType()),
                outboxEvent.getPartitionKey(),
                outboxEvent.getPayload());

        addHeader(record, "outbox_event_id", outboxEvent.getOutboxEventId());
        addHeader(record, "aggregate_type", outboxEvent.getAggregateType());
        addHeader(record, "aggregate_id", outboxEvent.getAggregateId());
        addHeader(record, "event_type", outboxEvent.getEventType());
        addHeader(record, "request_id", RequestIdContext.getCurrentRequestId().orElse(null));
        addSerializedHeaders(record, outboxEvent.getHeaders());

        try {
            SendResult<String, String> result = kafkaTemplate.send(record)
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            RecordMetadata metadata = result.getRecordMetadata();

            log.info("Published outbox event to Kafka topic={} partition={} offset={} eventId={} eventType={} key={}",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset(),
                    outboxEvent.getOutboxEventId(),
                    outboxEvent.getEventType(),
                    outboxEvent.getPartitionKey());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing outbox event to Kafka", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Unable to publish outbox event to Kafka", ex);
        }
    }

    private void addSerializedHeaders(ProducerRecord<String, String> record, String serializedHeaders) {
        if (serializedHeaders == null || serializedHeaders.isBlank()) {
            return;
        }

        try {
            Map<String, Object> headers = objectMapper.readValue(serializedHeaders, MAP_TYPE);
            headers.forEach((key, value) -> addHeader(record, key, value));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize outbox headers", ex);
        }
    }

    private void addHeader(ProducerRecord<String, String> record, String key, Object value) {
        if (value == null) {
            return;
        }
        record.headers().add(key, value.toString().getBytes(StandardCharsets.UTF_8));
    }
}
