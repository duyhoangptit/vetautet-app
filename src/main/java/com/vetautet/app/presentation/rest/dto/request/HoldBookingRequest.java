
package com.vetautet.app.presentation.rest.dto.request;

import com.vetautet.app.domain.payment.model.PaymentMethodCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldBookingRequest {

    @NotNull(message = "Departure ID is required")
    private UUID departureId;

    @Size(max = 30, message = "Booking channel cannot exceed 30 characters")
    private String bookingChannel;

    @NotBlank(message = "Customer full name is required")
    @Size(max = 255, message = "Customer full name cannot exceed 255 characters")
    private String customerFullName;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Invalid customer email format")
    @Size(max = 255, message = "Customer email cannot exceed 255 characters")
    private String customerEmail;

    @NotBlank(message = "Customer phone is required")
    @Size(max = 30, message = "Customer phone cannot exceed 30 characters")
    private String customerPhone;

    @NotNull(message = "Payment method is required")
    private PaymentMethodCode paymentMethodCode;

    @Size(max = 30, message = "Provider code cannot exceed 30 characters")
    private String providerCode;

    private Integer holdDurationMinutes;

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100, message = "Idempotency key cannot exceed 100 characters")
    private String idempotencyKey;

    @Valid
    @NotEmpty(message = "At least one booking item is required")
    private List<HoldBookingItemRequest> items;
}