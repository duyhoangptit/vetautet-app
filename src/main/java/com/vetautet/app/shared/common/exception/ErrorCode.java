package com.vetautet.app.shared.common.exception;

import lombok.Getter;


/**
 * Enum defining error codes for the application
 * Messages are externalized in i18n properties files
 */

@Getter
public enum ErrorCode {

    // Resource errors (1xxx)

    RESOURCE_NOT_FOUND("ERR-1001"),

    USER_NOT_FOUND("ERR-1002"),

    USER_NOT_FOUND_BY_EMAIL("ERR-1003"),

    RSA_KEY_PAIR_NOT_FOUND("ERR-1004"),

    AUTH_FLOW_TOKEN_NOT_FOUND("ERR-1005"),

    OTP_SESSION_NOT_FOUND("ERR-1006"),

    NOTIFICATION_TEMPLATE_NOT_FOUND("ERR-1007"),



    // Duplicate resource errors (2xxx)

    DUPLICATE_RESOURCE("ERR-2001"),

    DUPLICATE_EMAIL("ERR-2002"),

    DUPLICATE_USERNAME("ERR-2003"),



    // Validation errors (3xxx)

    INVALID_INPUT("ERR-3001"),

    INVALID_EMAIL_FORMAT("ERR-3002"),

    INVALID_USER_STATE("ERR-3003"),

    INVALID_STATUS_TRANSITION("ERR-3004"),

    INVALID_OTP("ERR-3005"),

    OTP_EXPIRED("ERR-3006"),

    OTP_MAX_ATTEMPTS_EXCEEDED("ERR-3007"),

    INVALID_AUTH_FLOW_TOKEN("ERR-3008"),

    AUTH_FLOW_TOKEN_EXPIRED("ERR-3009"),

    INVALID_ORDER_CURSOR("ERR-3010"),



    // Authentication/Authorization errors (4xxx)

    AUTHENTICATION_FAILED("ERR-4001"),

    INVALID_CREDENTIALS("ERR-4002"),

    TOKEN_EXPIRED("ERR-4003"),

    TOKEN_INVALID("ERR-4004"),

    UNAUTHORIZED_ACCESS("ERR-4005"),

    OTP_REQUIRED("ERR-4006"),

    RATE_LIMIT_EXCEEDED("ERR-4007"),

    ACCOUNT_LOCKED("ERR-4008"),



    // Business logic errors (5xxx)

    BUSINESS_RULE_VIOLATION("ERR-5001"),

    OPERATION_NOT_ALLOWED("ERR-5002"),

    IDEMPOTENCY_REQUEST_IN_PROGRESS("ERR-5003"),

    IDEMPOTENCY_KEY_REUSED("ERR-5004"),



    // System errors (9xxx)

    INTERNAL_SERVER_ERROR("ERR-9001"),

    DATABASE_ERROR("ERR-9002"),

    EXTERNAL_SERVICE_ERROR("ERR-9003");


    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

}

