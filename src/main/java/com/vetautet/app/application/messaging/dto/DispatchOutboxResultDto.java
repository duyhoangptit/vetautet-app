
package com.vetautet.app.application.messaging.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DispatchOutboxResultDto {
    private int requestedBatchSize;
    private int processedCount;
    private int publishedCount;
    private int failedCount;
    private int dlqCount;
    private List<DispatchedOutboxEventDto> events;
}