package com.vetautet.app.infrastructure.cache.bloomfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.vetautet.app.application.user.port.output.UserAvailabilityProbe;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.vetautet.app.infrastructure.config.TaskExecutionConfig;
import com.vetautet.app.infrastructure.persistence.jpa.entity.UserJpaEntity;
import com.vetautet.app.infrastructure.persistence.jpa.repository.UserJpaRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * One-time startup job that populates the user-availability Bloom filters
 * from the database, then flips the sync-state bucket read by
 * {@link RedissonUserAvailabilityProbeAdapter#isSynced()} from
 * {@link BloomFilterSyncState#SYNCING} to {@link BloomFilterSyncState#SYNCED}.
 * <p>
 * Runs {@link Async async} on its own single-thread executor
 * ({@link TaskExecutionConfig#USER_BLOOM_FILTER_SYNC_EXECUTOR}, which
 * carries the current {@code requestId}/MDC onto that thread via {@code
 * TaskExecutionConfig}'s decorator) so a slow sync (e.g. millions of
 * users) does not delay the app's readiness signal, which Spring Boot
 * publishes right after {@link ApplicationReadyEvent} listeners return.
 * Walks the table with keyset ("seek") pagination on the primary
 * key instead of {@code findAll(Pageable)}'s OFFSET paging, which would
 * otherwise degrade to O(n^2) as the offset grows on a large table. Each
 * page's usernames/emails are added to the Bloom filters with a single
 * bulk {@code add(Collection)} call instead of one round trip per value.
 * An optional delay between pages throttles sustained DB/Redis load so the
 * job does not starve request-time traffic sharing the same connection
 * pool/Redis instance.
 * <p>
 * If it fails partway (DB or Redis error), the state bucket is left
 * unset/{@code SYNCING} so callers keep using the database fallback instead
 * of trusting a half-populated filter.
 * Infrastructure layer.
 */
@Slf4j
@Component
public class UserBloomFilterSyncService {

    private static final UUID FIRST_BATCH_SENTINEL = new UUID(0L, 0L);

    private final UserJpaRepository userJpaRepository;
    private final RedissonClient redissonClient;
    private final RBloomFilter<String> userUsernameBloomFilter;
    private final RBloomFilter<String> userEmailBloomFilter;
    private final UserAvailabilityProbe userAvailabilityProbe;

    @Value("${app.user-availability.bloom-filter.sync-batch-size:500}")
    private int syncBatchSize;

    @Value("${app.user-availability.bloom-filter.batch-delay-ms:5}")
    private long batchDelayMs;

    public UserBloomFilterSyncService(
            UserJpaRepository userJpaRepository,
            RedissonClient redissonClient,
            UserAvailabilityProbe userAvailabilityProbe,
            @Qualifier(UserAvailabilityRedisKeys.USERNAME_BLOOM_FILTER_BEAN) RBloomFilter<String> userUsernameBloomFilter,
            @Qualifier(UserAvailabilityRedisKeys.EMAIL_BLOOM_FILTER_BEAN) RBloomFilter<String> userEmailBloomFilter) {
        this.userJpaRepository = userJpaRepository;
        this.redissonClient = redissonClient;
        this.userUsernameBloomFilter = userUsernameBloomFilter;
        this.userEmailBloomFilter = userEmailBloomFilter;
        this.userAvailabilityProbe = userAvailabilityProbe;
    }

    @Async(TaskExecutionConfig.USER_BLOOM_FILTER_SYNC_EXECUTOR)
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        try {
            if (this.userAvailabilityProbe.isSynced()) {
                log.debug("BloomFilter synced");
                return;
            }

            RBucket<String> stateBucket = redissonClient.getBucket(UserAvailabilityRedisKeys.SYNC_STATE_KEY);

            stateBucket.set(BloomFilterSyncState.SYNCING.name());

            long indexed = indexAllUsers();

            stateBucket.set(BloomFilterSyncState.SYNCED.name());
            log.info("User availability Bloom filter sync completed: {} users indexed", indexed);
        } catch (RedisException ex) {
            log.error("User availability Bloom filter sync failed due to Redis error, staying on DB fallback", ex);
        } catch (Exception ex) {
            log.error("User availability Bloom filter sync failed unexpectedly, staying on DB fallback", ex);
        }
    }

    /**
     * Walks the users table page by page using keyset pagination and bulk
     * Bloom filter inserts, sleeping {@code batchDelayMs} between pages.
     */
    private long indexAllUsers() {
        long indexed = 0;
        UUID lastId = FIRST_BATCH_SENTINEL;
        Pageable limit = PageRequest.of(0, syncBatchSize);

        List<UserJpaEntity> batch;
        do {
            batch = userJpaRepository.findByUserIdGreaterThanOrderByUserIdAsc(lastId, limit);
            if (batch.isEmpty()) {
                break;
            }

            addBatch(batch);
            indexed += batch.size();
            lastId = batch.get(batch.size() - 1).getUserId();

            if (batchDelayMs > 0 && batch.size() == syncBatchSize) {
                sleepBetweenBatches();
            }
            log.info("Finish batch {}", indexed);
        } while (batch.size() == syncBatchSize);

        return indexed;
    }

    /**
     * Throttles the sync loop so it does not monopolize the DB connection
     * pool / Redis while regular request traffic shares them. Restores the
     * interrupt flag and aborts the sync (via an unchecked exception caught
     * by {@link #syncOnStartup()}) rather than swallowing an interrupt,
     * e.g. on app shutdown.
     */
    private void sleepBetweenBatches() {
        try {
            Thread.sleep(batchDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("User availability Bloom filter sync interrupted", ex);
        }
    }

    private void addBatch(List<UserJpaEntity> batch) {
        List<String> usernames = new ArrayList<>(batch.size());
        List<String> emails = new ArrayList<>(batch.size());

        for (UserJpaEntity user : batch) {
            collectIfPresent(usernames, user.getUsername());
            collectIfPresent(emails, user.getEmail());
        }

        if (!usernames.isEmpty()) {
            userUsernameBloomFilter.add(usernames);
        }
        if (!emails.isEmpty()) {
            userEmailBloomFilter.add(emails);
        }
    }

    private void collectIfPresent(List<String> target, String rawValue) {
        if (StringUtils.hasText(rawValue)) {
            target.add(rawValue.trim().toLowerCase());
        }
    }
}
