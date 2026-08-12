package com.vetautet.app.infrastructure.idempotency;

/**
* Lifecycle states for an idempotent request.
*/
public enum IdempotencyStatus {
    PROCESSING,
    SUCCESS,
    FAILED
}