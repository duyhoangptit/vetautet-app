package com.vetautet.app.presentation.config.ratelimit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Binds per-scope rate-limit thresholds from {@code app.rate-limit.scopes.*}.
 * A new public endpoint only needs a new entry here plus a
 * {@code @RateLimit(scope = "...")} annotation - no new limiter class.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private Map<String, RateLimitSpec> scopes = new HashMap<>();

    @Data
    public static class RateLimitSpec {
        private int softLimitPerMinute;
        private int hardLimitPerHour;
        private long challengeTtlMinutes;
        private long blockTtlMinutes;
    }
}
