
package com.vetautet.app.application.messaging.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DispatchOutboxCommand {
    private Integer batchSize;
    private Instant dispatchTime;
}
