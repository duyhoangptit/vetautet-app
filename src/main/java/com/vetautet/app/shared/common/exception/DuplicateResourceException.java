package com.vetautet.app.shared.common.exception;

import lombok.Getter;

@Getter
public class DuplicateResourceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] messageArgs;

    public DuplicateResourceException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }
}
