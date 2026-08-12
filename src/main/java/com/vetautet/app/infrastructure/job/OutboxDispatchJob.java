package com.vetautet.app.infrastructure.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vetautet.app.application.messaging.dto.DispatchOutboxCommand;
import com.vetautet.app.application.messaging.dto.DispatchOutboxResultDto;
import com.vetautet.app.application.messaging.port.input.DispatchOutboxUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatchJob {

    private final DispatchOutboxUseCase dispatchOutboxUseCase;
    private final OutboxCircuitBreaker circuitBreaker;

    @Value("${app.outbox.dispatch.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.outbox.dispatch.fixed-delay-ms:1000}")
    public void dispatch() {
        if (!circuitBreaker.allowCall()) {
            log.warn("OutboxDispatchJob skipped: circuit breaker is OPEN");
            return;
        }

        try {
            DispatchOutboxResultDto result = dispatchOutboxUseCase.execute(
                    DispatchOutboxCommand.builder()
                            .batchSize(batchSize)
                            .build());

            if (result.getProcessedCount() > 0) {
                log.info("Outbox dispatch completed: processed={}, published={}, failed={}, dlq={}",
                        result.getProcessedCount(), result.getPublishedCount(),
                        result.getFailedCount(), result.getDlqCount());
            }

            // Record circuit breaker outcome: full failure = all processed events failed/dlq'd
            boolean allFailed = result.getProcessedCount() > 0 && result.getPublishedCount() == 0;
            if (allFailed) {
                circuitBreaker.recordFailure();
            } else {
                circuitBreaker.recordSuccess();
            }

        } catch (Exception ex) {
            log.error("Outbox dispatch job failed unexpectedly", ex);
            circuitBreaker.recordFailure();
        }
    }
}

 