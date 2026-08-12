package com.vetautet.app.shared.common.exception;

import lombok.Getter;

/**
 * Raised when an idempotency key is reused with a different request payload.
 */
@Getter
public class IdempotencyConflictException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] messageArgs;

    public IdempotencyConflictException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }
}