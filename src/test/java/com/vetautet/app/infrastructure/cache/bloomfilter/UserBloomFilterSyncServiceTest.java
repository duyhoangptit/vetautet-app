package com.vetautet.app.infrastructure.cache.bloomfilter;

import com.vetautet.app.infrastructure.persistence.jpa.entity.UserJpaEntity;
import com.vetautet.app.infrastructure.persistence.jpa.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBloomFilterSyncServiceTest {

    private static final UUID ZERO_UUID = new UUID(0L, 0L);

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBloomFilter<String> userUsernameBloomFilter;

    @Mock
    private RBloomFilter<String> userEmailBloomFilter;

    @Mock
    private RBucket<String> syncStateBucket;

    private UserBloomFilterSyncService service;

    @BeforeEach
    void setUp() {
        service = new UserBloomFilterSyncService(
                userJpaRepository, redissonClient, userUsernameBloomFilter, userEmailBloomFilter);
        ReflectionTestUtils.setField(service, "syncBatchSize", 2);
        ReflectionTestUtils.setField(service, "batchDelayMs", 0L);
    }

    private UserJpaEntity userWith(UUID id, String username, String email) {
        UserJpaEntity user = new UserJpaEntity();
        user.setUserId(id);
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }

    @Test
    void syncOnStartup_singlePartialBatch_indexesAllUsersAndMarksSynced() {
        when(redissonClient.<String>getBucket(UserAvailabilityRedisKeys.SYNC_STATE_KEY)).thenReturn(syncStateBucket);
        UserJpaEntity user = userWith(UUID.randomUUID(), "John_Doe", "John@Example.com");
        when(userJpaRepository.findByUserIdGreaterThanOrderByUserIdAsc(eq(ZERO_UUID), any(Pageable.class)))
                .thenReturn(List.of(user));

        service.syncOnStartup();

        verify(userUsernameBloomFilter).add(List.of("john_doe"));
        verify(userEmailBloomFilter).add(List.of("john@example.com"));
        verify(syncStateBucket).set(BloomFilterSyncState.SYNCING.name());
        verify(syncStateBucket).set(BloomFilterSyncState.SYNCED.name());
    }

    @Test
    void syncOnStartup_multiplePages_walksForwardByLastSeenId() {
        when(redissonClient.<String>getBucket(UserAvailabilityRedisKeys.SYNC_STATE_KEY)).thenReturn(syncStateBucket);
        UserJpaEntity first = userWith(UUID.fromString("00000000-0000-0000-0000-000000000001"), "alice", "alice@example.com");
        UserJpaEntity second = userWith(UUID.fromString("00000000-0000-0000-0000-000000000002"), "bob", "bob@example.com");
        UserJpaEntity third = userWith(UUID.fromString("00000000-0000-0000-0000-000000000003"), "carol", "carol@example.com");
        UserJpaEntity fourth = userWith(UUID.fromString("00000000-0000-0000-0000-000000000004"), "dave", "dave@example.com");

        when(userJpaRepository.findByUserIdGreaterThanOrderByUserIdAsc(eq(ZERO_UUID), any(Pageable.class)))
                .thenReturn(List.of(first, second));
        when(userJpaRepository.findByUserIdGreaterThanOrderByUserIdAsc(eq(second.getUserId()), any(Pageable.class)))
                .thenReturn(List.of(third, fourth));
        // 3rd page (lastId = fourth's id) is left unstubbed -> Mockito's default
        // answer returns an empty list for a List-returning method, which ends the loop

        service.syncOnStartup();

        verify(userUsernameBloomFilter).add(List.of("alice", "bob"));
        verify(userUsernameBloomFilter).add(List.of("carol", "dave"));
        verify(syncStateBucket).set(BloomFilterSyncState.SYNCED.name());
        // a full batch (size == syncBatchSize) always triggers one more lookup
        // to confirm there is nothing left
        verify(userJpaRepository).findByUserIdGreaterThanOrderByUserIdAsc(eq(fourth.getUserId()), any(Pageable.class));
    }

    @Test
    void syncOnStartup_skipsBlankUsername() {
        when(redissonClient.<String>getBucket(UserAvailabilityRedisKeys.SYNC_STATE_KEY)).thenReturn(syncStateBucket);
        UserJpaEntity user = userWith(UUID.randomUUID(), null, "no-username@example.com");
        when(userJpaRepository.findByUserIdGreaterThanOrderByUserIdAsc(eq(ZERO_UUID), any(Pageable.class)))
                .thenReturn(List.of(user));

        service.syncOnStartup();

        verify(userUsernameBloomFilter, never()).add(anyList());
        verify(userEmailBloomFilter).add(List.of("no-username@example.com"));
    }

    @Test
    void syncOnStartup_dbErrorPartway_doesNotMarkSynced() {
        when(redissonClient.<String>getBucket(UserAvailabilityRedisKeys.SYNC_STATE_KEY)).thenReturn(syncStateBucket);
        when(userJpaRepository.findByUserIdGreaterThanOrderByUserIdAsc(eq(ZERO_UUID), any(Pageable.class)))
                .thenThrow(new RuntimeException("db down"));

        service.syncOnStartup();

        verify(syncStateBucket).set(BloomFilterSyncState.SYNCING.name());
        verify(syncStateBucket, never()).set(BloomFilterSyncState.SYNCED.name());
    }

    @Test
    void syncOnStartup_redisErrorGettingBucket_doesNotThrow() {
        when(redissonClient.<String>getBucket(UserAvailabilityRedisKeys.SYNC_STATE_KEY))
                .thenThrow(new RedisException("connection refused"));

        service.syncOnStartup();
    }
}
