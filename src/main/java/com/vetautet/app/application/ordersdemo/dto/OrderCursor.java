package com.vetautet.app.application.ordersdemo.dto;

import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Opaque keyset-pagination cursor: the {@code id} (UUIDv7) of the boundary
 * row. Base64url-encoded so it's safe to pass as a query parameter.
 */
public record OrderCursor(UUID id) {

    public String encode() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static OrderCursor decode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AppLogicException(ErrorCode.INVALID_ORDER_CURSOR, String.valueOf(raw));
        }
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(raw);
            UUID id = UUID.fromString(new String(decodedBytes, StandardCharsets.UTF_8));
            return new OrderCursor(id);
        } catch (IllegalArgumentException ex) {
            throw new AppLogicException(ErrorCode.INVALID_ORDER_CURSOR, ex, raw);
        }
    }
}
