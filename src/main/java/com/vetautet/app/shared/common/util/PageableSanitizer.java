package com.vetautet.app.shared.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Last line of defense against unbounded pagination (OWASP API4:2023 -
 * Unrestricted Resource Consumption).
 * <p>
 * Controller-level checks are easy to forget or bypass (a new endpoint, an
 * internal batch job, a future caller that builds its own {@link Pageable}).
 * Applying {@link #capped(Pageable)} at the persistence-adapter boundary -
 * immediately before the Spring Data repository call - guarantees the query
 * that actually reaches the database is bounded, regardless of what any
 * upstream layer did or forgot to do.
 */
public final class PageableSanitizer {

    public static final int MAX_PAGE_SIZE = 100;

    private PageableSanitizer() {
    }

    /**
     * Returns {@code pageable} unchanged if its page size is already within
     * {@link #MAX_PAGE_SIZE}, otherwise a copy clamped to it (page number and
     * sort are preserved).
     */
    public static Pageable capped(Pageable pageable) {
        return capped(pageable, MAX_PAGE_SIZE);
    }

    public static Pageable capped(Pageable pageable, int maxPageSize) {
        if (pageable == null || pageable.getPageSize() <= maxPageSize) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), maxPageSize, pageable.getSort());
    }
}
