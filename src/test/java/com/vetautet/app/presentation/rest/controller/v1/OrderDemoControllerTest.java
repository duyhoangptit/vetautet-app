package com.vetautet.app.presentation.rest.controller.v1;

import com.vetautet.app.application.ordersdemo.dto.OrderKeysetPage;
import com.vetautet.app.application.ordersdemo.port.input.GetOrdersPageUseCase;
import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import com.vetautet.app.presentation.rest.dto.response.OrderKeysetPageResponse;
import com.vetautet.app.presentation.rest.mapper.OrderPresentationMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDemoControllerTest {

    @Mock
    private GetOrdersPageUseCase getOrdersPageUseCase;

    @Mock
    private HttpServletRequest httpRequest;

    private OrderDemoController controller;

    @BeforeEach
    void setUp() {
        controller = new OrderDemoController(getOrdersPageUseCase, new OrderPresentationMapper());
        when(httpRequest.getRequestURI()).thenReturn("/api/v1/orders-demo");
    }

    private Order order() {
        return Order.builder()
                .id(UUID.randomUUID())
                .orderCode("ORD-1")
                .customerName("A")
                .customerEmail("a@example.com")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .currency("VND")
                .quantity(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void listOrders_delegatesToUseCaseAndMapsResult() {
        OrderKeysetPage page = OrderKeysetPage.builder()
                .content(List.of(order()))
                .nextCursor("next")
                .prevCursor(null)
                .hasNext(true)
                .hasPrevious(false)
                .build();
        when(getOrdersPageUseCase.execute(null, null, 20)).thenReturn(page);

        ResponseEntity<BaseResponse<OrderKeysetPageResponse>> response =
                controller.listOrders(null, null, 20, httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        OrderKeysetPageResponse payload = response.getBody().getPayload();
        assertThat(payload.getContent()).hasSize(1);
        assertThat(payload.isHasNext()).isTrue();
        assertThat(payload.getNextCursor()).isEqualTo("next");
    }

    @Test
    void listOrders_sizeAboveMax_isCappedTo100BeforeCallingUseCase() {
        when(getOrdersPageUseCase.execute(eq(null), eq(null), anyInt()))
                .thenReturn(OrderKeysetPage.builder().content(List.of()).hasNext(false).hasPrevious(false).build());

        controller.listOrders(null, null, 999, httpRequest);

        verify(getOrdersPageUseCase).execute(null, null, 100);
    }
}
