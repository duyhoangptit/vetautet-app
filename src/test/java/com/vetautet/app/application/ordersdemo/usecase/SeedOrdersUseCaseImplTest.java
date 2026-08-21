package com.vetautet.app.application.ordersdemo.usecase;

import com.vetautet.app.application.ordersdemo.port.output.OrderBulkWriter;
import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeedOrdersUseCaseImplTest {

    @Mock
    private OrderBulkWriter orderBulkWriter;

    @Mock
    private FakeOrderDataFactory fakeOrderDataFactory;

    private SeedOrdersUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SeedOrdersUseCaseImpl(orderBulkWriter, fakeOrderDataFactory);
        when(fakeOrderDataFactory.generate(org.mockito.ArgumentMatchers.any(UUID.class), org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenAnswer(invocation -> Order.builder()
                        .id(invocation.getArgument(0))
                        .createdAt(invocation.getArgument(1))
                        .orderCode("ORD-X")
                        .customerName("A")
                        .customerEmail("a@example.com")
                        .status(OrderStatus.PENDING)
                        .totalAmount(BigDecimal.ONE)
                        .currency("VND")
                        .quantity(1)
                        .build());
        when(orderBulkWriter.insertBatch(anyList())).thenAnswer(invocation -> ((List<?>) invocation.getArgument(0)).size());
    }

    @Test
    void execute_countLessThanBatchSize_insertsOneBatchOfExactCount() {
        int inserted = useCase.execute(1200);

        assertThat(inserted).isEqualTo(1200);
        ArgumentCaptor<List<Order>> captor = ArgumentCaptor.forClass(List.class);
        verify(orderBulkWriter, times(1)).insertBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(1200);
    }

    @Test
    void execute_countSpanningMultipleBatches_insertsFullBatchesPlusRemainder() {
        int inserted = useCase.execute(12_000);

        assertThat(inserted).isEqualTo(12_000);
        verify(orderBulkWriter, times(3)).insertBatch(anyList());
    }

    @Test
    void execute_generatesStrictlyIncreasingTimestampsAcrossRows() {
        useCase.execute(3);

        ArgumentCaptor<Instant> timestampCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(fakeOrderDataFactory, times(3)).generate(org.mockito.ArgumentMatchers.any(UUID.class), timestampCaptor.capture());
        List<Instant> timestamps = timestampCaptor.getAllValues();
        assertThat(timestamps.get(0)).isBefore(timestamps.get(1));
        assertThat(timestamps.get(1)).isBefore(timestamps.get(2));
    }
}
