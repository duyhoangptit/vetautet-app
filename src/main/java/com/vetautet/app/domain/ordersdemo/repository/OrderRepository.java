package com.vetautet.app.domain.ordersdemo.repository;

import com.vetautet.app.domain.ordersdemo.model.Order;

import java.util.List;
import java.util.UUID;

/**
 * Output port for keyset-paginated reads over the demo {@code orders} table.
 * Every method returns up to {@code limit} rows ordered by {@code id}
 * (UUIDv7) - callers request {@code size + 1} rows to detect "has more"
 * without a separate COUNT query (see spec &sect;3.4).
 */
public interface OrderRepository {

    /** First page: no cursor, newest first. */
    List<Order> findFirstPage(int limit);

    /** Rows strictly older than {@code afterId} (id &lt; afterId), newest first. */
    List<Order> findNextPage(UUID afterId, int limit);

    /**
     * Rows strictly newer than {@code beforeId} (id &gt; beforeId), returned
     * oldest first (ascending) - the caller is responsible for reversing this
     * back to newest-first display order.
     */
    List<Order> findPrevPage(UUID beforeId, int limit);
}
