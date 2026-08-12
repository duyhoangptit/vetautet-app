package com.vetautet.app.presentation.rest.controller.v1;

import com.vetautet.app.application.user.dto.AvailabilityCheckType;
import com.vetautet.app.application.user.dto.AvailabilityResult;
import com.vetautet.app.application.user.port.input.CheckUserAvailabilityUseCase;
import com.vetautet.app.presentation.config.ratelimit.RateLimit;
import com.vetautet.app.presentation.rest.dto.response.AvailabilityResponse;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated endpoint that gives the registration form a
 * real-time hint about whether a username/email is already taken. Kept out
 * of {@link UserController} because that controller requires a bearer JWT
 * + RBAC while this route is deliberately {@code permitAll} (see
 * {@code SecurityConfig}) and protected instead by IP-based rate limiting.
 * Presentation layer - REST controller.
 */
@RestController
@RequestMapping("/api/v1/users/availability")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Availability", description = "Public best-effort username/email availability check")
public class UserAvailabilityController {

    private final CheckUserAvailabilityUseCase checkUserAvailabilityUseCase;

    @GetMapping
    @RateLimit(scope = "availability")
    @Operation(summary = "Check username/email availability",
            description = "Best-effort, IP rate-limited check used by the registration form")
    public ResponseEntity<BaseResponse<AvailabilityResponse>> checkAvailability(
            @Parameter(description = "username hoặc email") @RequestParam String type,
            @Parameter(description = "Giá trị đang gõ trên form đăng ký") @RequestParam String value) {
        AvailabilityCheckType checkType = AvailabilityCheckType.valueOf(type.trim().toUpperCase());
        AvailabilityResult result = checkUserAvailabilityUseCase.execute(checkType, value);

        AvailabilityResponse response = AvailabilityResponse.builder()
                .type(result.getType().name().toLowerCase())
                .value(result.getValue())
                .available(result.isAvailable())
                .build();

        log.debug("Availability check type={} available={}", response.getType(), response.isAvailable());
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
