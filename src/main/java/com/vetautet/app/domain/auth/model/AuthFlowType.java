package com.vetautet.app.domain.auth.model;

/**
* Auth flows that can issue links and OTP challenges.
*/
public enum AuthFlowType {
    LOGIN_2FA,
    REGISTER_ACTIVATION,
    PASSWORD_RESET
}