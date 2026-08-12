package com.vetautet.app.presentation.rest.controller.v1;

import com.vetautet.app.application.payment.dto.ConfirmPaymentResultDto;
import com.vetautet.app.application.payment.port.input.ConfirmPaymentUseCase;
import com.vetautet.app.infrastructure.idempotency.Idempotent;
import com.vetautet.app.presentation.config.RequireBearerAuth;
import com.vetautet.app.presentation.rest.dto.request.ConfirmPaymentRequest;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import com.vetautet.app.presentation.rest.dto.response.ConfirmPaymentResponse;
import com.vetautet.app.presentation.rest.mapper.BookingFlowPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@RequireBearerAuth
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Payment Flow", description = "APIs for confirming payment and finalizing ticket booking")
public class PaymentController {

    private final ConfirmPaymentUseCase confirmPaymentUseCase;
    private final BookingFlowPresentationMapper mapper;

    @PostMapping("/confirm")
    @Idempotent(key = "#request.paymentTransactionId.toString()", operation = "confirm-payment", ttlHours = 24, hashFields = {"#request"})
    @Operation(summary = "Confirm payment", description = "Confirm a payment transaction and finalize booking inventory")
    public ResponseEntity<BaseResponse<ConfirmPaymentResponse>> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request,
            HttpServletRequest httpRequest) {
        ConfirmPaymentResultDto result = confirmPaymentUseCase.execute(mapper.toCommand(request));
        BaseResponse<ConfirmPaymentResponse> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(),
                "Payment confirmed successfully",
                mapper.toResponse(result),
                httpRequest.getRequestURI());

        return ResponseEntity.ok(baseResponse);
    }
}