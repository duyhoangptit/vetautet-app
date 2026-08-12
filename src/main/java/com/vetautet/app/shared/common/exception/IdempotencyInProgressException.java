package com.vetautet.app.shared.common.exception;

import lombok.Getter;

@Getter
public class IdempotencyInProgressException extends RuntimeException {
    private final ErrorCode errorCode;
    private final transient Object[] messageArgs;

    public IdempotencyInProgressException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = new Object[0];
    }

    public IdempotencyInProgressException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    public IdempotencyInProgressException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getCode(), cause);
        this.errorCode = errorCode;
        this.messageArgs = new Object[0];
    }

    public IdempotencyInProgressException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode.getCode(), cause);
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }
}
