package com.vetautet.app.shared.common.exception;

import lombok.Getter;

/**
 * Exception thrown when attempting to create a duplicate resource
 */
@Getter
public class AppLogicException extends RuntimeException {
    private final ErrorCode errorCode;
    private final transient Object[] messageArgs;

    public AppLogicException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = new Object[0];
    }

    public AppLogicException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    public AppLogicException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getCode(), cause);
        this.errorCode = errorCode;
        this.messageArgs = new Object[0];
    }

    public AppLogicException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode.getCode(), cause);
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }
}
