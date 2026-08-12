package com.vetautet.app.infrastructure.logging;

import java.util.concurrent.TimeUnit;

import com.vetautet.app.shared.common.context.RequestIdContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class ExecutionLoggingAspect {

    @Value("${app.logging.execution.slow-threshold-ms:1000}")
    private long slowThresholdMs;

    @Around("""
            execution(public * com.vetautet.app..*(..))
            && (@within(org.springframework.stereotype.Service) || @within(org.springframework.stereotype.Component) || @within(org.springframework.stereotype.Controller))
            && !@within(org.aspectj.lang.annotation.Aspect)
            """)
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.nanoTime();

        try {
            logStart(methodName);
            Object result = joinPoint.proceed();
            long durationMs = elapsedMs(startTime);
            logCompletion(methodName, durationMs);
            return result;
        } catch (Throwable ex) {
            long durationMs = elapsedMs(startTime);
            log.warn("Function {} failed after {} ms: {}", methodName, durationMs, ex.getMessage(), ex);
            throw ex;
        }
    }

    private boolean isRequestIdNotExist() {
        return RequestIdContext.getCurrentRequestId().isEmpty();
    }

    private void logStart(String methodName) {
        if (isRequestIdNotExist()) return;
        log.debug("Start function {}", methodName);
    }

    private void logCompletion(String methodName, long durationMs) {
        if (isRequestIdNotExist()) return;
        if (durationMs > slowThresholdMs) {
            log.warn("End function {} in {} ms, exceeded threshold {} ms", methodName, durationMs, slowThresholdMs);
            return;
        }

        log.debug("End function {} in {} ms", methodName, durationMs);
    }

    private long elapsedMs(long startTime) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    }
}
