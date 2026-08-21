package com.vetautet.app.application.ordersdemo.dto;

import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderCursorTest {

    @Test
    void encodeThenDecode_roundTripsToSameId() {
        UUID id = UUID.randomUUID();
        OrderCursor cursor = new OrderCursor(id);

        String encoded = cursor.encode();
        OrderCursor decoded = OrderCursor.decode(encoded);

        assertThat(decoded.id()).isEqualTo(id);
    }

    @Test
    void decode_nullCursor_throwsInvalidOrderCursor() {
        assertThatThrownBy(() -> OrderCursor.decode(null))
                .isInstanceOf(AppLogicException.class)
                .extracting(ex -> ((AppLogicException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_CURSOR);
    }

    @Test
    void decode_blankCursor_throwsInvalidOrderCursor() {
        assertThatThrownBy(() -> OrderCursor.decode("   "))
                .isInstanceOf(AppLogicException.class);
    }

    @Test
    void decode_notValidBase64_throwsInvalidOrderCursor() {
        assertThatThrownBy(() -> OrderCursor.decode("not-valid-base64!!!"))
                .isInstanceOf(AppLogicException.class)
                .extracting(ex -> ((AppLogicException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_CURSOR);
    }

    @Test
    void decode_validBase64ButNotAUuid_throwsInvalidOrderCursor() {
        String tampered = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("not-a-uuid".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThatThrownBy(() -> OrderCursor.decode(tampered))
                .isInstanceOf(AppLogicException.class)
                .extracting(ex -> ((AppLogicException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_CURSOR);
    }
}
