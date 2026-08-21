package com.vetautet.app.application.ordersdemo.port.output;

import com.vetautet.app.domain.ordersdemo.model.Order;

import java.util.List;

/** Output port for bulk-writing seed data, bypassing JPA for throughput. */
public interface OrderBulkWriter {
    /** @return number of rows attempted (see implementation notes on why this isn't a driver-reported affected-row count) */
    int insertBatch(List<Order> orders);
}
