package com.vetautet.app.presentation.rest.controller.v1;

import com.vetautet.app.application.auth.dto.*;
import com.vetautet.app.application.auth.port.input.*;
import com.vetautet.app.application.auth.port.input.ForgotPasswordUseCase;
import com.vetautet.app.presentation.config.RequireBearerAuth;
import com.vetautet.app.presentation.config.ratelimit.RateLimit;
import com.vetautet.app.presentation.rest.dto.request.*;
import com.vetautet.app.presentation.rest.dto.response.*;
import com.vetautet.app.presentation.rest.mapper.AuthPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
* REST Controller for Authentication
* Presentation layer - handles HTTP requests
* Uses Clean Architecture use cases
*/
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "APIs for authentication and authorization")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RegisterUseCase registerUseCase;
    private final ActivateAccountUseCase activateAccountUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final VerifyOtpLoginUseCase verifyOtpLoginUseCase;
    private final AuthPresentationMapper mapper;

    @PostMapping("/register")
    @RateLimit(scope = "register")
    @Operation(summary = "User registration", description = "Register a new user account")
    public ResponseEntity<BaseResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        log.info("Register request for email: {}", request.getEmail());

        RegisterResultDto registerResult = registerUseCase.execute(mapper.toCommand(request));
        RegisterResponse response = mapper.toRegisterResponse(registerResult);
        BaseResponse<RegisterResponse> baseResponse = BaseResponse.success(
                HttpStatus.CREATED.value(),
                "Register successful",
                response,
                httpRequest.getRequestURI());

        log.info("User {} registered successfully with ID: {}",
                request.getEmail(), response.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(baseResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user credentials and create an OTP challenge for login verification.")
    public ResponseEntity<BaseResponse<OtpChallengeResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Portal", required = false, defaultValue = "A") String portal) {
        log.info("Login request for email: {}", request.getEmail());

        request.setPortal(portal); // Set portal from header to request
        OtpChallengeDto otpChallengeDto = loginUseCase.execute(mapper.toCommand(request));
        OtpChallengeResponse response = mapper.toResponse(otpChallengeDto);
        BaseResponse<OtpChallengeResponse> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(),
                "OTP challenge created",
                response,
                httpRequest.getRequestURI());

        log.info("Login challenge created for user {} with otp session {}",
                request.getEmail(), response.getOtpSessionId());

        return ResponseEntity.ok(baseResponse);
    }

    @PostMapping("/verify-otp-login")
    @Operation(summary = "Verify login OTP", description = "Verify OTP for login and issue JWT token")
    public ResponseEntity<BaseResponse<TokenResponse>> verifyOtpLogin(
            @Valid @RequestBody VerifyOtpLoginRequest request,
            HttpServletRequest httpRequest) {
        TokenDto tokenDto = verifyOtpLoginUseCase.execute(mapper.toCommand(request));
        TokenResponse response = mapper.toResponse(tokenDto);
        BaseResponse<TokenResponse> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(),
                "Login verified successfully",
                response,
                httpRequest.getRequestURI());

        return ResponseEntity.ok(baseResponse);
    }

    @PostMapping("/register/activate")
    @RateLimit(scope = "registerActivate")
    @Operation(summary = "Activate registered account", description = "Send or verify OTP to activate a registered account")
    public ResponseEntity<BaseResponse<ActivateAccountResponse>> activateAccount(
            @Valid @RequestBody ActivateAccountRequest request,
            HttpServletRequest httpRequest) {
        ActivateAccountResultDto result = activateAccountUseCase.execute(mapper.toCommand(request));
        ActivateAccountResponse response = mapper.toResponse(result);
        BaseResponse<ActivateAccountResponse> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(),
                result.isActivated() ? "Account activated successfully" : "OTP sent successfully",
                response,
                httpRequest.getRequestURI());

        return ResponseEntity.ok(baseResponse);
    }

    @PostMapping("/forgot-password")
    @RateLimit(scope = "forgotPassword")
    @Operation(summary = "Forgot password", description = "Create a password reset link for the user")
    public ResponseEntity<BaseResponse<AuthFlowLinkResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        AuthFlowLinkDto result = forgotPasswordUseCase.execute(mapper.toCommand(request));
        AuthFlowLinkResponse response = mapper.toResponse(result);
        BaseResponse<AuthFlowLinkResponse> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(),
                "Password reset link created",
                response,
                httpRequest.getRequestURI());

        return ResponseEntity.ok(baseResponse);
    }

    @PostMapping("/reset-password")
    @RateLimit(scope = "resetPassword")
    @Operation(summary = "Reset password", description = "Send OTP or verify OTP to complete password reset")
    public ResponseEntity<BaseResponse<ResetPasswordResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        ResetPasswordResultDto result = resetPasswordUseCase.execute(mapper.toCommand(request));
        ResetPasswordResponse response = mapper.toResponse(result);
        BaseResponse<ResetPasswordResponse> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(),
                result.isPasswordChanged() ? "Password reset successful" : "OTP sent successfully",
                response,
                httpRequest.getRequestURI());

        return ResponseEntity.ok(baseResponse);
    }

    @PostMapping("/logout")
    @RequireBearerAuth
    @Operation(summary = "User logout", description = "Logout the current session and revoke only the RSA key used by the current JWT")
    public ResponseEntity<BaseResponse<Map<String, String>>> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        String userId = jwt.getSubject();
        String keyId = String.valueOf(jwt.getHeaders().get("kid"));

        log.info("Logout request for user: {}", userId);

        logoutUseCase.execute(userId, keyId);
        Map<String, String> payload = Map.of(
                "message", "Logout successful. Current session token has been revoked.",
                "userId", userId,
                "keyId", keyId);
        BaseResponse<Map<String, String>> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(),
                "Logout successful",
                payload,
                httpRequest.getRequestURI());

        log.info("User {} logged out successfully", userId);

        return ResponseEntity.ok(baseResponse);
    }
}
 