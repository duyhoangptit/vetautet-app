package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.OrderJpaEntity;
import com.vetautet.app.infrastructure.persistence.jpa.repository.OrderJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.OrderEntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryAdapterTest {

    @Mock
    private OrderJpaRepository orderJpaRepository;

    private OrderRepositoryAdapter adapter;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new OrderRepositoryAdapter(orderJpaRepository, new OrderEntityMapper());
    }

    private OrderJpaEntity entity() {
        return OrderJpaEntity.builder()
                .id(id)
                .orderCode("ORD-1")
                .customerName("A")
                .customerEmail("a@example.com")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ONE)
                .currency("VND")
                .quantity(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void findFirstPage_mapsEntitiesToDomain() {
        when(orderJpaRepository.findFirstPage(21)).thenReturn(List.of(entity()));

        List<Order> result = adapter.findFirstPage(21);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(id);
    }

    @Test
    void findNextPage_delegatesWithSameArguments() {
        UUID cursorId = UUID.randomUUID();
        when(orderJpaRepository.findNextPage(cursorId, 21)).thenReturn(List.of(entity()));

        adapter.findNextPage(cursorId, 21);

        verify(orderJpaRepository).findNextPage(cursorId, 21);
    }

    @Test
    void findPrevPage_delegatesWithSameArguments() {
        UUID cursorId = UUID.randomUUID();
        when(orderJpaRepository.findPrevPage(cursorId, 21)).thenReturn(List.of(entity()));

        adapter.findPrevPage(cursorId, 21);

        verify(orderJpaRepository).findPrevPage(cursorId, 21);
    }

    @Test
    void findFirstPage_limitAboveMaxQueryLimit_isClampedTo101() {
        when(orderJpaRepository.findFirstPage(101)).thenReturn(List.of());

        adapter.findFirstPage(5000);

        verify(orderJpaRepository).findFirstPage(101);
    }
}
