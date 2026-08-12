package com.vetautet.app.infrastructure.cache.bloomfilter;

import com.vetautet.app.application.user.dto.AvailabilityCheckType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedissonUserAvailabilityProbeAdapterTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBloomFilter<String> userUsernameBloomFilter;

    @Mock
    private RBloomFilter<String> userEmailBloomFilter;

    @Mock
    private RBucket<String> syncStateBucket;

    private RedissonUserAvailabilityProbeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedissonUserAvailabilityProbeAdapter(redissonClient, userUsernameBloomFilter, userEmailBloomFilter);
    }

    @Test
    void isSynced_stateIsSynced_returnsTrue() {
        when(redissonClient.<String>getBucket(UserAvailabilityRedisKeys.SYNC_STATE_KEY)).thenReturn(syncStateBucket);
        when(syncStateBucket.get()).thenReturn(BloomFilterSyncState.SYNCED.name());

        assertThat(adapter.isSynced()).isTrue();
    }

    @Test
    void isSynced_stateIsSyncing_returnsFalse() {
        when(redissonClient.<String>getBucket(UserAvailabilityRedisKeys.SYNC_STATE_KEY)).thenReturn(syncStateBucket);
        when(syncStateBucket.get()).thenReturn(BloomFilterSyncState.SYNCING.name());

        assertThat(adapter.isSynced()).isFalse();
    }

    @Test
    void isSynced_noStateYet_returnsFalse() {
        when(redissonClient.<String>getBucket(UserAvailabilityRedisKeys.SYNC_STATE_KEY)).thenReturn(syncStateBucket);
        when(syncStateBucket.get()).thenReturn(null);

        assertThat(adapter.isSynced()).isFalse();
    }

    @Test
    void isSynced_redisUnavailable_failsSafeToFalse() {
        when(redissonClient.<String>getBucket(UserAvailabilityRedisKeys.SYNC_STATE_KEY))
                .thenThrow(new RedisException("connection refused"));

        assertThat(adapter.isSynced()).isFalse();
    }

    @Test
    void mightExist_username_delegatesToUsernameBloomFilter() {
        when(userUsernameBloomFilter.contains("john_doe")).thenReturn(true);

        assertThat(adapter.mightExist(AvailabilityCheckType.USERNAME, "john_doe")).isTrue();
    }

    @Test
    void mightExist_email_delegatesToEmailBloomFilter() {
        when(userEmailBloomFilter.contains("john@example.com")).thenReturn(false);

        assertThat(adapter.mightExist(AvailabilityCheckType.EMAIL, "john@example.com")).isFalse();
    }
}
