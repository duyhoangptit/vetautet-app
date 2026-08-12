package com.vetautet.app.presentation.rest.mapper;

import org.springframework.stereotype.Component;

import com.vetautet.app.application.auth.dto.ActivateAccountCommand;
import com.vetautet.app.application.auth.dto.ActivateAccountResultDto;
import com.vetautet.app.application.auth.dto.AuthFlowLinkDto;
import com.vetautet.app.application.auth.dto.ForgotPasswordCommand;
import com.vetautet.app.application.auth.dto.LoginCommand;
import com.vetautet.app.application.auth.dto.OtpChallengeDto;
import com.vetautet.app.application.auth.dto.RegisterCommand;
import com.vetautet.app.application.auth.dto.RegisterResultDto;
import com.vetautet.app.application.auth.dto.ResetPasswordCommand;
import com.vetautet.app.application.auth.dto.ResetPasswordResultDto;
import com.vetautet.app.application.auth.dto.TokenDto;
import com.vetautet.app.application.auth.dto.VerifyOtpLoginCommand;
import com.vetautet.app.application.user.dto.UserDto;
import com.vetautet.app.presentation.rest.dto.request.ActivateAccountRequest;
import com.vetautet.app.presentation.rest.dto.request.ForgotPasswordRequest;
import com.vetautet.app.presentation.rest.dto.request.LoginRequest;
import com.vetautet.app.presentation.rest.dto.request.RegisterRequest;
import com.vetautet.app.presentation.rest.dto.request.ResetPasswordRequest;
import com.vetautet.app.presentation.rest.dto.request.VerifyOtpLoginRequest;
import com.vetautet.app.presentation.rest.dto.response.ActivateAccountResponse;
import com.vetautet.app.presentation.rest.dto.response.AuthFlowLinkResponse;
import com.vetautet.app.presentation.rest.dto.response.OtpChallengeResponse;
import com.vetautet.app.presentation.rest.dto.response.RegisterResponse;
import com.vetautet.app.presentation.rest.dto.response.ResetPasswordResponse;
import com.vetautet.app.presentation.rest.dto.response.TokenResponse;
import com.vetautet.app.shared.common.util.MaskUtil;

/**
* Mapper for auth presentation layer
* Converts between REST DTOs and Application DTOs
*/
@Component
public class AuthPresentationMapper {

    /**
     * Convert LoginRequest to LoginCommand
     */
    public LoginCommand toCommand(LoginRequest request) {
        if (request == null) {
            return null;
        }

        return LoginCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .portal(request.getPortal())
                .build();
    }

    /**
     * Convert RegisterRequest to RegisterCommand.
     */
    public RegisterCommand toCommand(RegisterRequest request) {
        if (request == null) {
            return null;
        }

        return RegisterCommand.builder()
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword())
                .mobileCountryCode(request.getMobileCountryCode())
                .mobileNumber(request.getMobileNumber())
                .avatarUrl(request.getAvatarUrl())
                .build();
    }

    public ForgotPasswordCommand toCommand(ForgotPasswordRequest request) {
        if (request == null) {
            return null;
        }

        return ForgotPasswordCommand.builder()
                .email(request.getEmail())
                .build();
    }

    public ResetPasswordCommand toCommand(ResetPasswordRequest request) {
        if (request == null) {
            return null;
        }

        return ResetPasswordCommand.builder()
                .resetToken(request.getResetToken())
                .newPassword(request.getNewPassword())
                .otpSessionId(request.getOtpSessionId())
                .otpCode(request.getOtpCode())
                .build();
    }

    public VerifyOtpLoginCommand toCommand(VerifyOtpLoginRequest request) {
        if (request == null) {
            return null;
        }

        return VerifyOtpLoginCommand.builder()
                .otpSessionId(request.getOtpSessionId())
                .otpCode(request.getOtpCode())
                .build();
    }

    public ActivateAccountCommand toCommand(ActivateAccountRequest request) {
        if (request == null) {
            return null;
        }

        return ActivateAccountCommand.builder()
                .activationToken(request.getActivationToken())
                .otpSessionId(request.getOtpSessionId())
                .otpCode(request.getOtpCode())
                .build();
    }

    /**
     * Convert TokenDto to TokenResponse
     */
    public TokenResponse toResponse(TokenDto dto) {
        if (dto == null) {
            return null;
        }

        return TokenResponse.builder()
                .accessToken(dto.getAccessToken())
                .tokenType(dto.getTokenType())
                .expiresAt(dto.getExpiresAt())
                .keyId(dto.getKeyId())
                .build();
    }

    public OtpChallengeResponse toResponse(OtpChallengeDto dto) {
        if (dto == null) {
            return null;
        }

        return OtpChallengeResponse.builder()
                .otpSessionId(dto.getOtpSessionId())
                .flowType(dto.getFlowType().name())
                .maskedEmail(MaskUtil.maskEmail(dto.getDestination()))
                .expiresAt(dto.getExpiresAt())
                .referenceToken(dto.getReferenceToken())
                .build();
    }

    public AuthFlowLinkResponse toResponse(AuthFlowLinkDto dto) {
        if (dto == null) {
            return null;
        }

        return AuthFlowLinkResponse.builder()
                .token(dto.getToken())
                .link(dto.getLink())
                .expiresAt(dto.getExpiresAt())
                .build();
    }

    /**
     * Convert UserDto to RegisterResponse.
     */
    public RegisterResponse toRegisterResponse(RegisterResultDto dto) {
        if (dto == null || dto.getUser() == null) {
            return null;
        }

        UserDto user = dto.getUser();

        return RegisterResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobileCountryCode(user.getMobileCountryCode())
                .mobileNumber(user.getMobileNumber())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .isOnboarding(user.getIsOnboarding())
                .createdDate(user.getCreatedDate())
                .lastModifiedDate(user.getLastModifiedDate())
                .createdBy(user.getCreatedBy())
                .lastModifiedBy(user.getLastModifiedBy())
                .activationLink(dto.getActivationLink() != null ? dto.getActivationLink().getLink() : null)
                .activationLinkExpiresAt(dto.getActivationLink() != null ? dto.getActivationLink().getExpiresAt() : null)
                .build();
    }

    public ActivateAccountResponse toResponse(ActivateAccountResultDto dto) {
        if (dto == null) {
            return null;
        }

        return ActivateAccountResponse.builder()
                .activated(dto.isActivated())
                .userId(dto.getUserId())
                .otpChallenge(toResponse(dto.getOtpChallenge()))
                .build();
    }

    public ResetPasswordResponse toResponse(ResetPasswordResultDto dto) {
        if (dto == null) {
            return null;
        }

        return ResetPasswordResponse.builder()
                .passwordChanged(dto.isPasswordChanged())
                .otpChallenge(toResponse(dto.getOtpChallenge()))
                .build();
    }
}
 