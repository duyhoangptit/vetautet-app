package com.vetautet.app.application.ordersdemo.usecase;

import com.vetautet.app.application.ordersdemo.dto.OrderCursor;
import com.vetautet.app.application.ordersdemo.dto.OrderKeysetPage;
import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import com.vetautet.app.domain.ordersdemo.repository.OrderRepository;
import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrdersPageUseCaseImplTest {

    @Mock
    private OrderRepository orderRepository;

    private GetOrdersPageUseCaseImpl useCase;

    private final UUID id1 = UUID.fromString("00000000-0000-7000-8000-000000000001");
    private final UUID id2 = UUID.fromString("00000000-0000-7000-8000-000000000002");
    private final UUID id3 = UUID.fromString("00000000-0000-7000-8000-000000000003");

    private Order order(UUID id) {
        return Order.builder()
                .id(id)
                .orderCode("ORD-" + id)
                .customerName("Test Customer")
                .customerEmail("test@example.com")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .currency("VND")
                .quantity(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @BeforeEach
    void setUp() {
        useCase = new GetOrdersPageUseCaseImpl(orderRepository);
    }

    @Test
    void execute_noCursor_hasMoreRows_returnsFirstPageWithNextCursorOnly() {
        // size=2 -> probe limit 3; repo returns 3 rows meaning there's a 4th+ row beyond
        when(orderRepository.findFirstPage(3)).thenReturn(List.of(order(id1), order(id2), order(id3)));

        OrderKeysetPage page = useCase.execute(null, null, 2);

        assertThat(page.content()).extracting(Order::getId).containsExactly(id1, id2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.prevCursor()).isNull();
        assertThat(OrderCursor.decode(page.nextCursor()).id()).isEqualTo(id2);
    }

    @Test
    void execute_noCursor_lastPage_hasNextFalseAndNullCursor() {
        when(orderRepository.findFirstPage(3)).thenReturn(List.of(order(id1), order(id2)));

        OrderKeysetPage page = useCase.execute(null, null, 2);

        assertThat(page.content()).hasSize(2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void execute_afterCursor_delegatesToFindNextPage_andHasPreviousIsTrue() {
        String after = new OrderCursor(id1).encode();
        when(orderRepository.findNextPage(id1, 3)).thenReturn(List.of(order(id2), order(id3)));

        OrderKeysetPage page = useCase.execute(after, null, 2);

        assertThat(page.content()).extracting(Order::getId).containsExactly(id2, id3);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isFalse();
        assertThat(OrderCursor.decode(page.prevCursor()).id()).isEqualTo(id2);
    }

    @Test
    void execute_beforeCursor_reversesAscendingRepoResultBackToDescendingDisplayOrder() {
        String before = new OrderCursor(id3).encode();
        // repo returns ascending (oldest-first): id1 then id2, for a 2-row page (no more beyond)
        when(orderRepository.findPrevPage(id3, 3)).thenReturn(List.of(order(id1), order(id2)));

        OrderKeysetPage page = useCase.execute(null, before, 2);

        assertThat(page.content()).extracting(Order::getId).containsExactly(id2, id1);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.prevCursor()).isNull();
    }

    @Test
    void execute_bothAfterAndBeforeSupplied_throwsInvalidOrderCursor() {
        String after = new OrderCursor(id1).encode();
        String before = new OrderCursor(id2).encode();

        assertThatThrownBy(() -> useCase.execute(after, before, 20))
                .isInstanceOf(AppLogicException.class)
                .extracting(ex -> ((AppLogicException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_CURSOR);
    }

    @Test
    void execute_sizeAboveMaxPageSize_isCappedBeforeQuerying() {
        when(orderRepository.findFirstPage(101)).thenReturn(List.of(order(id1)));

        useCase.execute(null, null, 999);

        org.mockito.Mockito.verify(orderRepository).findFirstPage(101);
    }
}
