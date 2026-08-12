package com.vetautet.app.infrastructure.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "spring.cache.redis")
public class CacheProperties {
    private long defaultTtl;
    private Map<String, Long> caches;
}
