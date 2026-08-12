package com.vetautet.app.infrastructure.job;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
* Lightweight in-memory circuit breaker for the outbox dispatch pipeline.
*
* <pre>
*  CLOSED ──(consecutive failures >= threshold)──► OPEN
*  OPEN   ──(cooldown elapsed)───────────────────► HALF_OPEN
*  HALF_OPEN ──(probe succeeds)──────────────────► CLOSED
*  HALF_OPEN ──(probe fails)─────────────────────► OPEN
* </pre>
*
* A "failure" is recorded when an entire dispatch batch produces zero published
* events (all events failed or the publisher threw unexpectedly).
* A "success" is recorded when at least one event in the batch was published.
*/
@Slf4j
@Component
public class OutboxCircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final long cooldownMs;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong openedAt = new AtomicLong(0);

    public OutboxCircuitBreaker(
            @Value("${app.outbox.circuit-breaker.failure-threshold:5}") int failureThreshold,
            @Value("${app.outbox.circuit-breaker.cooldown-ms:60000}") long cooldownMs) {
        this.failureThreshold = failureThreshold;
        this.cooldownMs = cooldownMs;
    }

    /**
     * Returns {@code true} if the caller is allowed to attempt a dispatch.
     * When OPEN, returns {@code false} until the cooldown elapses, at which
     * point the state transitions to HALF_OPEN and one probe call is permitted.
     */
    public boolean allowCall() {
        State current = state.get();

        if (current == State.CLOSED || current == State.HALF_OPEN) {
            return true;
        }

        // OPEN: check if cooldown has elapsed
        if (System.currentTimeMillis() - openedAt.get() >= cooldownMs) {
            if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                log.info("OutboxCircuitBreaker: OPEN → HALF_OPEN (cooldown elapsed, probing)");
            }
            return true;
        }

        return false;
    }

    /**
     * Must be called after a successful dispatch batch (at least one event published).
     */
    public void recordSuccess() {
        consecutiveFailures.set(0);
        if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
            log.info("OutboxCircuitBreaker: HALF_OPEN → CLOSED (probe succeeded)");
        }
    }

    /**
     * Must be called after a fully-failed dispatch batch or an unexpected exception.
     */
    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();

        if (state.get() == State.HALF_OPEN) {
            state.set(State.OPEN);
            openedAt.set(System.currentTimeMillis());
            log.warn("OutboxCircuitBreaker: HALF_OPEN → OPEN (probe failed)");
            return;
        }

        if (state.get() == State.CLOSED && failures >= failureThreshold) {
            if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                openedAt.set(System.currentTimeMillis());
                log.warn("OutboxCircuitBreaker: CLOSED → OPEN after {} consecutive failures", failures);
            }
        }
    }

    public State getState() {
        return state.get();
    }
}
 