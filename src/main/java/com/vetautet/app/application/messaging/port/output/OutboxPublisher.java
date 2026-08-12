package com.vetautet.app.application.messaging.port.output;

import com.vetautet.app.domain.messaging.model.OutboxEvent;

public interface OutboxPublisher {

    void publish(OutboxEvent outboxEvent);
}