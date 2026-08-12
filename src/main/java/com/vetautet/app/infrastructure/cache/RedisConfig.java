package com.vetautet.app.infrastructure.cache;

import io.lettuce.core.ClientOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
* Redis configuration for caching and session management.
* Connects to Redis server at the configured host and port.
*/
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.timeout:500ms}")
    private Duration redisTimeout;

    /**
     * Redis connection factory using Lettuce client.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);

        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.setPassword(redisPassword);
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(redisTimeout)
                .shutdownTimeout(Duration.ofMillis(100))
                .clientOptions(ClientOptions.builder()
                        .autoReconnect(true)
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                        .build())
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);

        // QUAN TRỌNG: Tắt | bật tính năng tự động xác thực kết nối ngay lúc khởi tạo Bean
        factory.setValidateConnection(true);

        return factory;
    }

    /**
     * Redis template for generic Redis operations.
     * Uses String serializer for keys and JSON serializer for values.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Cache manager for Spring @Cacheable annotations.
     * Configures default TTL and serialization.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, CacheProperties cacheProperties) {
        RedisCacheConfiguration defaultConfig = buildCacheConfig(cacheProperties.getDefaultTtl());

        Map<String, RedisCacheConfiguration> perCacheConfigs = new HashMap<>();
        cacheProperties.getCaches().forEach((cacheName, ttl) -> perCacheConfigs.put(cacheName, buildCacheConfig(ttl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfigs)
                .transactionAware()
                .build();
    }

    private RedisCacheConfiguration buildCacheConfig(long ttlSeconds) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(RedisSerializer.json()))
                .disableCachingNullValues();
    }

    // Nếu app cần sống kể cả Redis down, nên cấu hình cache theo hướng fail-open
    // @Cacheable sẽ log lỗi nhưng ko throw exception, tránh ảnh hưởng đến luồng chính của app
    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
        return new CustomCacheErrorHandler();
    }
}
 
