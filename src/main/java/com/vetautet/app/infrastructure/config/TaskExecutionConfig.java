package com.vetautet.app.infrastructure.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.vetautet.app.shared.common.context.RequestIdContext;

/**
 * Central place for background-task infrastructure: enables {@code @Scheduled}
 * and {@code @Async}, and defines the {@link TaskDecorator} that carries the
 * current {@link RequestIdContext} (and therefore the MDC {@code requestId})
 * onto whichever worker thread picks up the task, so logs emitted from
 * background work stay correlated with the request that triggered it. Every
 * custom executor defined here should apply this decorator via
 * {@code setTaskDecorator(...)}.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class TaskExecutionConfig {

    /**
     * Backs {@code UserBloomFilterSyncService#syncOnStartup()}. A single
     * one-shot job at a time is all this ever needs; core/max pool size of
     * 1 keeps it from competing for threads with anything else.
     */
    public static final String USER_BLOOM_FILTER_SYNC_EXECUTOR = "userBloomFilterSyncExecutor";

    @Bean
    public TaskDecorator requestIdTaskDecorator() {
        return runnable -> {
            RequestIdContext.Snapshot snapshot = RequestIdContext.snapshot();
            return () -> RequestIdContext.runWithRequestId(snapshot.requestId(), runnable::run);
        };
    }

    @Bean(USER_BLOOM_FILTER_SYNC_EXECUTOR)
    public Executor userBloomFilterSyncExecutor(TaskDecorator requestIdTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("bloom-sync-");
        executor.setTaskDecorator(requestIdTaskDecorator);
        executor.initialize();
        return executor;
    }
}
