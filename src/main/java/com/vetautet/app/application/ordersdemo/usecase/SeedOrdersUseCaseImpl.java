package com.vetautet.app.application.ordersdemo.usecase;

import com.vetautet.app.application.ordersdemo.port.input.SeedOrdersUseCase;
import com.vetautet.app.application.ordersdemo.port.output.OrderBulkWriter;
import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.shared.common.util.Uuid7Generator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Spreads {@code count} synthetic orders' {@code created_at} evenly across
 * the last {@link #SEED_SPAN_DAYS} days, strictly increasing row-by-row, and
 * mints each row's UUIDv7 {@code id} from that same timestamp
 * ({@link Uuid7Generator#generate(Instant)}) - so {@code id} ordering matches
 * {@code created_at} ordering exactly, the same way real insert-time
 * UUIDv7 generation would.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeedOrdersUseCaseImpl implements SeedOrdersUseCase {

    private static final int BATCH_SIZE = 5000;
    private static final int SEED_SPAN_DAYS = 180;
    private static final int PROGRESS_LOG_INTERVAL = 100_000;

    private final OrderBulkWriter orderBulkWriter;
    private final FakeOrderDataFactory fakeOrderDataFactory;

    @Override
    public int execute(int count) {
        Instant baseTime = Instant.now().minus(SEED_SPAN_DAYS, ChronoUnit.DAYS);
        long spanMillis = Duration.ofDays(SEED_SPAN_DAYS).toMillis();

        int totalInserted = 0;
        List<Order> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < count; i++) {
            Instant createdAt = baseTime.plusMillis(i * spanMillis / count);
            UUID id = Uuid7Generator.generate(createdAt);
            batch.add(fakeOrderDataFactory.generate(id, createdAt));

            if (batch.size() == BATCH_SIZE) {
                totalInserted += orderBulkWriter.insertBatch(batch);
                batch.clear();
                if (totalInserted % PROGRESS_LOG_INTERVAL == 0) {
                    log.info("Seeded {} / {} orders", totalInserted, count);
                }
            }
        }
        if (!batch.isEmpty()) {
            totalInserted += orderBulkWriter.insertBatch(batch);
        }

        log.info("Finished seeding {} orders", totalInserted);
        return totalInserted;
    }
}
