package com.vetautet.app.infrastructure.ordersdemo.seed;

import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSeedJdbcWriterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private OrderSeedJdbcWriter writer;

    @BeforeEach
    void setUp() {
        writer = new OrderSeedJdbcWriter(jdbcTemplate);
    }

    private Order order(UUID id) {
        return Order.builder()
                .id(id)
                .orderCode("ORD-1")
                .customerName("A")
                .customerEmail("a@example.com")
                .customerPhone("0900000000")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .currency("VND")
                .quantity(1)
                .paymentMethod("COD")
                .shippingAddress("1 Le Loi")
                .shippingCity("Ha Noi")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void insertBatch_buildsOneJdbcBatchRowPerOrder_andReturnsInputSize() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<Order> orders = List.of(order(id1), order(id2));
        when(jdbcTemplate.batchUpdate(anyString(), anyList())).thenReturn(new int[]{1, 1});

        int inserted = writer.insertBatch(orders);

        assertThat(inserted).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> argsCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(anyString(), argsCaptor.capture());
        List<Object[]> capturedArgs = argsCaptor.getValue();
        assertThat(capturedArgs).hasSize(2);
        assertThat(capturedArgs.get(0)[0]).isEqualTo(id1);
        assertThat(capturedArgs.get(1)[0]).isEqualTo(id2);
    }

    @Test
    void insertBatch_emptyList_doesNotCallJdbcTemplate() {
        int inserted = writer.insertBatch(List.of());

        assertThat(inserted).isZero();
        org.mockito.Mockito.verifyNoInteractions(jdbcTemplate);
    }
}
