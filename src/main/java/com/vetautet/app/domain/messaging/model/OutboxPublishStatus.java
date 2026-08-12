package com.vetautet.app.domain.messaging.model;

public enum OutboxPublishStatus {
    NEW,
    PUB,
    FAI,
    /** Dead-letter: exceeded max retries, requires manual intervention. */
    DLQ
}