package com.vetautet.app.infrastructure.cache.bloomfilter;

/**
 * Lifecycle state of the one-time startup sync between the database and the
 * user-availability Bloom filters.
 * Infrastructure layer.
 */
public enum BloomFilterSyncState {
    NOT_SYNCED,
    SYNCING,
    SYNCED
}
