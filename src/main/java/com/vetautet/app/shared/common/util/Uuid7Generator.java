package com.vetautet.app.shared.common.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Generates RFC 9562 UUIDv7 values: a 48-bit big-endian Unix-epoch-millisecond
 * timestamp in the high bits, followed by version/variant marker bits and
 * random fill. Because the timestamp is the leading bit-field, UUIDv7 values
 * sort monotonically with wall-clock time and are still globally unique -
 * exactly the property {@code OrderRepositoryAdapter}'s keyset pagination
 * relies on to use {@code id} alone as both sort key and tie-breaker (see
 * docs/superpowers/specs/2026-08-21-orders-keyset-pagination-demo-design.md).
 * <p>
 * No same-millisecond monotonic counter is implemented - within one
 * millisecond, relative order among generated ids is randomized. Acceptable
 * here: the seeder spaces synthetic timestamps milliseconds apart
 * specifically to avoid this in bulk-seeded data.
 */
public final class Uuid7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long VERSION_7 = 0x7L;
    private static final long VARIANT_RFC4122 = 0x2L;
    private static final long TIMESTAMP_MASK_48_BITS = 0xFFFFFFFFFFFFL;
    private static final long RAND_A_MASK_12_BITS = 0x0FFFL;
    private static final long RAND_B_MASK_62_BITS = 0x3FFFFFFFFFFFFFFFL;

    private Uuid7Generator() {
    }

    /** Generates a UUIDv7 using the current wall-clock time. */
    public static UUID generate() {
        return generate(Instant.now());
    }

    /**
     * Generates a UUIDv7 embedding the given timestamp (truncated to
     * millisecond precision) instead of {@code Instant.now()} - used by the
     * demo-data seeder to spread synthetic ids across a wide time range
     * instead of clustering every seeded row around "now".
     */
    public static UUID generate(Instant timestamp) {
        long unixTimeMillis = timestamp.toEpochMilli();

        byte[] randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);

        long randA = (((randomBytes[0] & 0xFFL) << 4) | ((randomBytes[1] & 0xF0L) >>> 4))
                & RAND_A_MASK_12_BITS;

        long mostSigBits = ((unixTimeMillis & TIMESTAMP_MASK_48_BITS) << 16)
                | (VERSION_7 << 12)
                | randA;

        long randB = (((randomBytes[2] & 0xFFL) << 56)
                | ((randomBytes[3] & 0xFFL) << 48)
                | ((randomBytes[4] & 0xFFL) << 40)
                | ((randomBytes[5] & 0xFFL) << 32)
                | ((randomBytes[6] & 0xFFL) << 24)
                | ((randomBytes[7] & 0xFFL) << 16)
                | ((randomBytes[8] & 0xFFL) << 8)
                | (randomBytes[9] & 0xFFL))
                & RAND_B_MASK_62_BITS;

        long leastSigBits = (VARIANT_RFC4122 << 62) | randB;

        return new UUID(mostSigBits, leastSigBits);
    }

    /** Extracts the millisecond-precision timestamp embedded in a UUIDv7's leading 48 bits. */
    public static Instant extractTimestamp(UUID uuid) {
        long unixTimeMillis = uuid.getMostSignificantBits() >>> 16;
        return Instant.ofEpochMilli(unixTimeMillis);
    }
}
