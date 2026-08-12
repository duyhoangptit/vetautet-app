package com.vetautet.app.infrastructure.cache;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
* Service for Redis operations using both RedisTemplate and Redisson.
* Provides caching, distributed locking, and other Redis functionality.
*/
@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    public RedisService(RedisTemplate<String, Object> redisTemplate, RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
    }

    // ==================== Basic Cache Operations ====================

    /**
     * Set a value in Redis with TTL.
     */
    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * Set a value in Redis without TTL.
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * Get a value from Redis.
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Delete a key from Redis.
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * Check if a key exists in Redis.
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * Set expiration time for a key.
     */
    public Boolean expire(String key, Duration timeout) {
        return redisTemplate.expire(key, timeout);
    }

    // ==================== Redisson Advanced Operations ====================

    /**
     * Get a distributed lock with default timeout (30 seconds).
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * Try to acquire a lock with timeout.
     *
     * @param lockKey   The lock key
     * @param waitTime  Maximum time to wait for the lock
     * @param leaseTime Lock auto-release time
     * @return true if lock acquired, false otherwise
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Unlock a distributed lock.
     */
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * Set value using Redisson with TTL.
     */
    public <T> void setWithRedisson(String key, T value, Duration ttl) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Get value using Redisson.
     */
    public <T> T getWithRedisson(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    /**
     * Delete key using Redisson.
     */
    public boolean deleteWithRedisson(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.delete();
    }

    /**
     * Execute operation with distributed lock.
     *
     * @param lockKey   The lock key
     * @param operation The operation to execute
     * @param waitTime  Maximum time to wait for the lock
     * @param leaseTime Lock auto-release time
     * @return true if operation executed successfully, false if lock not acquired
     */
    public boolean executeWithLock(String lockKey, Runnable operation, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(waitTime, leaseTime, unit)) {
                try {
                    operation.run();
                    return true;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
 