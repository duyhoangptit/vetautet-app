package com.vetautet.app.domain.user.model;

/**
 * Enum representing User status
 */
public enum UserStatus {
    ACTIVE("ACT"),
    INACTIVE("INA"),
    PENDING("PND"),
    SUSPENDED("SUS"),
    DELETED("DEL");

    private final String code;

    UserStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static UserStatus fromCode(String code) {
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown user status code: " + code);
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean canBeModified() {
        return this != DELETED;
    }
}
