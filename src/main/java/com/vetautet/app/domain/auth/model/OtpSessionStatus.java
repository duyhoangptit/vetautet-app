package com.vetautet.app.domain.auth.model;

/**
* Lifecycle status for OTP sessions.
*/
public enum OtpSessionStatus {
    ACTIVE,
    VERIFIED,
    INACTIVE,
    EXPIRED
}