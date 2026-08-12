package com.vetautet.app.domain.user.exception;

import com.vetautet.app.shared.common.exception.ErrorCode;
import lombok.Getter;

/**
* Domain exception for duplicate email
*/
@Getter
public class DuplicateEmailException extends RuntimeException {
    private final ErrorCode errorCode;
    private final transient Object[] messageArgs;

    public DuplicateEmailException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }
}
 