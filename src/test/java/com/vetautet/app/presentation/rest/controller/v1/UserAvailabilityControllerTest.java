package com.vetautet.app.presentation.rest.controller.v1;

import com.vetautet.app.application.user.dto.AvailabilityCheckType;
import com.vetautet.app.application.user.dto.AvailabilityResult;
import com.vetautet.app.application.user.port.input.CheckUserAvailabilityUseCase;
import com.vetautet.app.presentation.rest.dto.response.AvailabilityResponse;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAvailabilityControllerTest {

    @Mock
    private CheckUserAvailabilityUseCase checkUserAvailabilityUseCase;

    @InjectMocks
    private UserAvailabilityController controller;

    @Test
    void checkAvailability_validUsernameType_returnsAvailableResponse() {
        when(checkUserAvailabilityUseCase.execute(AvailabilityCheckType.USERNAME, "john_doe"))
                .thenReturn(AvailabilityResult.builder()
                        .type(AvailabilityCheckType.USERNAME)
                        .value("john_doe")
                        .available(true)
                        .build());

        ResponseEntity<BaseResponse<AvailabilityResponse>> response =
                controller.checkAvailability("username", "john_doe");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        AvailabilityResponse payload = response.getBody().getPayload();
        assertThat(payload.getType()).isEqualTo("username");
        assertThat(payload.getValue()).isEqualTo("john_doe");
        assertThat(payload.isAvailable()).isTrue();
    }

    @Test
    void checkAvailability_typeIsCaseInsensitive_parsesToEnum() {
        when(checkUserAvailabilityUseCase.execute(AvailabilityCheckType.EMAIL, "john@example.com"))
                .thenReturn(AvailabilityResult.builder()
                        .type(AvailabilityCheckType.EMAIL)
                        .value("john@example.com")
                        .available(false)
                        .build());

        ResponseEntity<BaseResponse<AvailabilityResponse>> response =
                controller.checkAvailability("EMAIL", "john@example.com");

        assertThat(response.getBody().getPayload().isAvailable()).isFalse();
    }

    @Test
    void checkAvailability_unknownType_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> controller.checkAvailability("phone", "0900000000"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
