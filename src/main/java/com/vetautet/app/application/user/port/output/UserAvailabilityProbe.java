package com.vetautet.app.application.user.port.output;

import com.vetautet.app.application.user.dto.AvailabilityCheckType;

/**
 * Output port for a fast, best-effort existence probe (e.g. a Bloom filter)
 * used ahead of the database when checking username/email availability.
 * Application layer - port (implemented by infrastructure layer).
 */
public interface UserAvailabilityProbe {

    /**
     * Whether the probe's underlying index has finished its initial sync
     * with the database and can be trusted for lookups.
     */
    boolean isSynced();

    /**
     * Best-effort existence check for an already-normalized (trim +
     * lowercase) value. May return false positives but must never return a
     * false negative.
     */
    boolean mightExist(AvailabilityCheckType type, String normalizedValue);
}
