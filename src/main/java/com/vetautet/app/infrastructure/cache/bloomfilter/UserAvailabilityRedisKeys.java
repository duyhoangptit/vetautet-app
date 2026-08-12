package com.vetautet.app.infrastructure.cache.bloomfilter;

/**
 * Redis key / bean-name constants shared between the Bloom filter beans,
 * the startup sync job, and the probe adapter for the user-availability
 * check. Kept package-private since all three consumers live here.
 * Infrastructure layer.
 */
final class UserAvailabilityRedisKeys {

    static final String USERNAME_BLOOM_FILTER_BEAN = "userUsernameBloomFilter";
    static final String EMAIL_BLOOM_FILTER_BEAN = "userEmailBloomFilter";

    static final String USERNAME_BLOOM_FILTER_NAME = "user:availability:bloom:username";
    static final String EMAIL_BLOOM_FILTER_NAME = "user:availability:bloom:email";

    static final String SYNC_STATE_KEY = "user:availability:sync-state";

    private UserAvailabilityRedisKeys() {
    }
}
