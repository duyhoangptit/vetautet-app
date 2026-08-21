package com.vetautet.app.application.ordersdemo.port.input;

import com.vetautet.app.application.ordersdemo.dto.OrderKeysetPage;

/**
 * Input port for the orders-demo keyset pagination endpoint. Exactly one of
 * {@code afterCursor} / {@code beforeCursor} may be non-blank; both blank
 * means "first page". No page-number parameter exists by design - next/prev
 * only (see docs/keyset-pagination-spec.md).
 */
public interface GetOrdersPageUseCase {
    OrderKeysetPage execute(String afterCursor, String beforeCursor, int size);
}
