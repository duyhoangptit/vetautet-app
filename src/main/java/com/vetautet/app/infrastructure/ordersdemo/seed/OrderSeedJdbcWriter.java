package com.vetautet.app.infrastructure.ordersdemo.seed;

import com.vetautet.app.application.ordersdemo.port.output.OrderBulkWriter;
import com.vetautet.app.domain.ordersdemo.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

/**
 * Raw JDBC batch insert for seed data - deliberately bypasses
 * {@code OrderJpaRepository}/Hibernate. 2,000,000 rows through a JPA
 * persistence context would grow unbounded memory and slow down with
 * dirty-checking overhead; a plain batched {@code PreparedStatement} does
 * not have that cost.
 * <p>
 * {@code @Transactional} is required here, not optional: this project sets
 * {@code hikari.auto-commit: false} (see application.yml) so that connection
 * borrowing defers to Spring-managed transaction boundaries. Without an
 * explicit transaction, {@code JdbcTemplate.batchUpdate()} still executes
 * the statements, but nothing commits them - HikariCP rolls back the
 * uncommitted work when the connection returns to the pool, silently
 * discarding every row despite the call "succeeding" (no exception, correct
 * row count returned). One transaction per {@link #insertBatch} call keeps
 * each commit scoped to a single ~5,000-row chunk rather than one
 * multi-minute transaction spanning the entire seed run.
 */
@Component
@RequiredArgsConstructor
public class OrderSeedJdbcWriter implements OrderBulkWriter {

    private static final String INSERT_SQL = """
            INSERT INTO orders
                (id, order_code, customer_name, customer_email, customer_phone, status,
                 total_amount, currency, quantity, payment_method, shipping_address,
                 shipping_city, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public int insertBatch(List<Order> orders) {
        if (orders.isEmpty()) {
            return 0;
        }

        List<Object[]> batchArgs = orders.stream().map(this::toArgs).toList();
        // Postgres' JDBC driver commonly reports Statement.SUCCESS_NO_INFO (-2)
        // per row for batched inserts rather than an exact affected-row count,
        // so summing jdbcTemplate.batchUpdate()'s result isn't reliable here -
        // the input size is the accurate "rows attempted" count.
        jdbcTemplate.batchUpdate(INSERT_SQL, batchArgs);
        return orders.size();
    }

    private Object[] toArgs(Order order) {
        return new Object[]{
                order.getId(),
                order.getOrderCode(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getQuantity(),
                order.getPaymentMethod(),
                order.getShippingAddress(),
                order.getShippingCity(),
                order.getNotes(),
                Timestamp.from(order.getCreatedAt()),
                Timestamp.from(order.getUpdatedAt())
        };
    }
}
