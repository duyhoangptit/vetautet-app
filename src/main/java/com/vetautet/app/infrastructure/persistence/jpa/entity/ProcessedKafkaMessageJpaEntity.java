
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.messaging.model.KafkaMessageProcessStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_kafka_messages", uniqueConstraints = {
        @UniqueConstraint(name = "uk_processed_kafka_message", columnNames = {"consumer_group", "topic_name", "partition_no", "message_offset"})
}, indexes = {
        @Index(name = "idx_processed_kafka_messages_key", columnList = "topic_name,event_key")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedKafkaMessageJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "processed_message_id", nullable = false, updatable = false)
    private UUID processedMessageId;

    @Column(name = "consumer_group", nullable = false, length = 100)
    private String consumerGroup;

    @Column(name = "topic_name", nullable = false, length = 100)
    private String topicName;

    @Column(name = "partition_no", nullable = false)
    private Integer partitionNo;

    @Column(name = "message_offset", nullable = false)
    private Long messageOffset;

    @Column(name = "event_key", length = 255)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false, length = 3)
    private KafkaMessageProcessStatus processStatus;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;
}
