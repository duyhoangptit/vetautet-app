package com.vetautet.app.domain.auth.model;

/**
* Lifecycle status for auth flow link tokens.
*/
public enum AuthFlowTokenStatus {
    ACTIVE,
    USED,
    INACTIVE,
    EXPIRED
}