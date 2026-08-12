package com.vetautet.app.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.vetautet.app.shared.common.context.RequestIdContext;

@Configuration
@EnableScheduling
public class TaskExecutionConfig {

    @Bean
    public TaskDecorator requestIdTaskDecorator() {
        return runnable -> {
            RequestIdContext.Snapshot snapshot = RequestIdContext.snapshot();
            return () -> RequestIdContext.runWithRequestId(snapshot.requestId(), runnable::run);
        };
    }
}