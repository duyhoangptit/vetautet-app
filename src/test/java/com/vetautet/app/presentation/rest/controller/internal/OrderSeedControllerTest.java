package com.vetautet.app.presentation.rest.controller.internal;

import com.vetautet.app.application.ordersdemo.port.input.SeedOrdersUseCase;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSeedControllerTest {

    @Mock
    private SeedOrdersUseCase seedOrdersUseCase;

    @Mock
    private HttpServletRequest httpRequest;

    private OrderSeedController controller;

    @BeforeEach
    void setUp() {
        controller = new OrderSeedController(seedOrdersUseCase);
        when(httpRequest.getRequestURI()).thenReturn("/internal/orders-demo/seed");
    }

    @Test
    void seed_delegatesToUseCase_andReturnsInsertedCount() {
        when(seedOrdersUseCase.execute(2_000_000)).thenReturn(2_000_000);

        ResponseEntity<BaseResponse<Integer>> response = controller.seed(2_000_000, httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getPayload()).isEqualTo(2_000_000);
    }
}
