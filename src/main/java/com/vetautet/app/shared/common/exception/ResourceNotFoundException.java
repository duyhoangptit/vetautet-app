package com.vetautet.app.shared.common.exception;

import lombok.Getter;

/**
 * Exception thrown when a requested resource is not found
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;
    private final transient Object[] messageArgs;

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = new Object[0];
    }

    public ResourceNotFoundException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    public ResourceNotFoundException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getCode(), cause);
        this.errorCode = errorCode;
        this.messageArgs = new Object[0];
    }

    public ResourceNotFoundException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode.getCode(), cause);
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }
}
