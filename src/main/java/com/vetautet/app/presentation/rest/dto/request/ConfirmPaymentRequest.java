
package com.vetautet.app.presentation.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentRequest {

    @NotNull(message = "Payment transaction ID is required")
    private UUID paymentTransactionId;

    @Size(max = 100, message = "Provider transaction ID cannot exceed 100 characters")
    private String providerTransactionId;

    @Size(max = 1000, message = "Provider payment URL cannot exceed 1000 characters")
    private String providerPaymentUrl;
}