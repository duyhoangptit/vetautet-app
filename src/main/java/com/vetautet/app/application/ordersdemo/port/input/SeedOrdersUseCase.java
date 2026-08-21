package com.vetautet.app.application.ordersdemo.port.input;

/** Bulk-seeds the orders-demo table with synthetic rows. Dev/local use only. */
public interface SeedOrdersUseCase {
    /** @return number of rows attempted to insert */
    int execute(int count);
}
