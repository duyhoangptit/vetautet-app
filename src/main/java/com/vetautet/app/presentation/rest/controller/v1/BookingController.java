package com.vetautet.app.presentation.rest.controller.v1;

import com.vetautet.app.application.booking.dto.HoldBookingResultDto;
import com.vetautet.app.application.booking.port.input.HoldBookingUseCase;
import com.vetautet.app.infrastructure.idempotency.Idempotent;
import com.vetautet.app.presentation.config.RequireBearerAuth;
import com.vetautet.app.presentation.rest.dto.request.HoldBookingRequest;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import com.vetautet.app.presentation.rest.dto.response.HoldBookingResponse;
import com.vetautet.app.presentation.rest.mapper.BookingFlowPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@RequireBearerAuth
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Booking Flow", description = "APIs for holding ticket inventory and creating initial bookings")
public class BookingController {

    private final HoldBookingUseCase holdBookingUseCase;
    private final BookingFlowPresentationMapper mapper;

    @PostMapping("/hold")
    @Idempotent(key = "#request.idempotencyKey", operation = "hold-booking", ttlHours = 24, hashFields = {"#request"})
    @Operation(summary = "Hold booking", description = "Hold seat inventory, create booking order, and create initial payment transaction")
    public ResponseEntity<BaseResponse<HoldBookingResponse>> holdBooking(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody HoldBookingRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = UUID.fromString(jwt.getSubject());
        HoldBookingResultDto result = holdBookingUseCase.execute(mapper.toCommand(userId, request));
        BaseResponse<HoldBookingResponse> baseResponse = BaseResponse.success(
                HttpStatus.CREATED.value(),
                "Booking hold created successfully",
                mapper.toResponse(result),
                httpRequest.getRequestURI());

        return ResponseEntity.status(HttpStatus.CREATED).body(baseResponse);
    }
}