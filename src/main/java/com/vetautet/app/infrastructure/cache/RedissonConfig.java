package com.vetautet.app.infrastructure.cache;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.EqualJitterDelay;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
* Redisson configuration for advanced Redis features.
* Provides distributed locks, distributed collections, and other advanced Redis
* functionality.
*/
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * Redisson client for advanced Redis operations.
     * Supports distributed locks, maps, sets, queues, and more.
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        // Configure single server mode
        String address = String.format("redis://%s:%d", redisHost, redisPort);

        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase)
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(2)
                .setConnectTimeout(5000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryDelay(new EqualJitterDelay(Duration.ofMillis(500), Duration.ofSeconds(2)));

        // Set password if provided
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.setPassword(redisPassword);
        }

        // Use JSON codec for serialization
        config.setCodec(new JsonJacksonCodec());

        return Redisson.create(config);
    }
}
 