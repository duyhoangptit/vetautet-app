package com.vetautet.app.infrastructure.cache.bloomfilter;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines the two Redisson Bloom filters backing the user-availability
 * check (one per {@link com.vetautet.app.application.user.dto.AvailabilityCheckType}).
 * {@code tryInit} only takes effect the first time a given filter is
 * created in Redis, so it is safe (and a no-op) to call on every app
 * startup.
 * Infrastructure layer.
 */
@Configuration
public class UserBloomFilterConfig {

    @Value("${app.user-availability.bloom-filter.expected-insertions:500000}")
    private long expectedInsertions;

    @Value("${app.user-availability.bloom-filter.false-positive-probability:0.01}")
    private double falsePositiveProbability;

    @Bean(UserAvailabilityRedisKeys.USERNAME_BLOOM_FILTER_BEAN)
    public RBloomFilter<String> userUsernameBloomFilter(RedissonClient redissonClient) {
        return initBloomFilter(redissonClient, UserAvailabilityRedisKeys.USERNAME_BLOOM_FILTER_NAME);
    }

    @Bean(UserAvailabilityRedisKeys.EMAIL_BLOOM_FILTER_BEAN)
    public RBloomFilter<String> userEmailBloomFilter(RedissonClient redissonClient) {
        return initBloomFilter(redissonClient, UserAvailabilityRedisKeys.EMAIL_BLOOM_FILTER_NAME);
    }

    private RBloomFilter<String> initBloomFilter(RedissonClient redissonClient, String name) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(name);
        bloomFilter.tryInit(expectedInsertions, falsePositiveProbability);
        return bloomFilter;
    }
}
