package com.vetautet.app.shared.common.exception;

import lombok.Getter;

/**
 * Raised when a caller exceeds the allowed request rate for a protected operation.
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] messageArgs;

    public RateLimitExceededException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }
}
