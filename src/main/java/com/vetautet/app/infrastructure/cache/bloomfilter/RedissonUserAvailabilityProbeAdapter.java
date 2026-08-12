package com.vetautet.app.infrastructure.cache.bloomfilter;

import com.vetautet.app.application.user.dto.AvailabilityCheckType;
import com.vetautet.app.application.user.port.output.UserAvailabilityProbe;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Reads the two Bloom filters populated by {@link UserBloomFilterSyncService}
 * to answer username/email existence checks without hitting the database.
 * <p>
 * {@link #isSynced()} fails safe: any Redis error is treated as "not
 * synced" so {@code CheckUserAvailabilityUseCaseImpl} falls back to the
 * database instead of guessing. {@link #mightExist} is only ever called
 * right after {@link #isSynced()} returned true, so a Redis error there is
 * left to propagate as an unexpected error rather than silently guessing.
 * Infrastructure layer - adapter for {@link UserAvailabilityProbe}.
 */
@Slf4j
@Component
public class RedissonUserAvailabilityProbeAdapter implements UserAvailabilityProbe {

    private final RedissonClient redissonClient;
    private final RBloomFilter<String> userUsernameBloomFilter;
    private final RBloomFilter<String> userEmailBloomFilter;

    public RedissonUserAvailabilityProbeAdapter(
            RedissonClient redissonClient,
            @Qualifier(UserAvailabilityRedisKeys.USERNAME_BLOOM_FILTER_BEAN) RBloomFilter<String> userUsernameBloomFilter,
            @Qualifier(UserAvailabilityRedisKeys.EMAIL_BLOOM_FILTER_BEAN) RBloomFilter<String> userEmailBloomFilter) {
        this.redissonClient = redissonClient;
        this.userUsernameBloomFilter = userUsernameBloomFilter;
        this.userEmailBloomFilter = userEmailBloomFilter;
    }

    @Override
    public boolean isSynced() {
        try {
            RBucket<String> bucket = redissonClient.getBucket(UserAvailabilityRedisKeys.SYNC_STATE_KEY);
            String state = bucket.get();
            return StringUtils.hasText(state) && BloomFilterSyncState.SYNCED.name().equals(state);
        } catch (RedisException ex) {
            log.warn("Redis unavailable, treating user-availability probe as not synced: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean mightExist(AvailabilityCheckType type, String normalizedValue) {
        return switch (type) {
            case USERNAME -> userUsernameBloomFilter.contains(normalizedValue);
            case EMAIL -> userEmailBloomFilter.contains(normalizedValue);
        };
    }
}
