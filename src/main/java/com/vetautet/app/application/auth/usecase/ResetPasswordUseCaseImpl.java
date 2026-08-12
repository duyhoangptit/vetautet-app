package com.vetautet.app.application.auth.usecase;

import com.vetautet.app.application.auth.dto.ResetPasswordCommand;
import com.vetautet.app.application.auth.dto.ResetPasswordResultDto;
import com.vetautet.app.application.auth.port.input.ResetPasswordUseCase;
import com.vetautet.app.application.auth.port.output.PasswordHashEncoder;
import com.vetautet.app.application.auth.service.AuthFlowLinkService;
import com.vetautet.app.application.auth.service.OtpService;
import com.vetautet.app.domain.auth.model.AuthFlowToken;
import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.domain.auth.model.OtpSession;
import com.vetautet.app.domain.user.model.User;
import com.vetautet.app.domain.user.repository.UserRepository;
import com.vetautet.app.infrastructure.ratelimit.RateLimited;
import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ResetPasswordUseCaseImpl implements ResetPasswordUseCase {

    private final AuthFlowLinkService authFlowLinkService;
    private final OtpService otpService;
    private final UserRepository userRepository;
    private final PasswordHashEncoder passwordHashEncoder;

    @Override
    @RateLimited(key = "#command.resetToken", operation = "auth-reset-password", permits = 10, periodSeconds = 900)
    public ResetPasswordResultDto execute(ResetPasswordCommand command) {
        AuthFlowToken authFlowToken = authFlowLinkService.validateActiveToken(command.getResetToken(),
                AuthFlowType.PASSWORD_RESET);

        if (command.getOtpSessionId() == null || !StringUtils.hasText(command.getOtpCode())) {
            String pendingPasswordHash = passwordHashEncoder.encode(command.getNewPassword());
            return ResetPasswordResultDto.builder()
                    .passwordChanged(false)
                    .otpChallenge(otpService.sendOtp(
                            authFlowToken.getUserId() != null ? authFlowToken.getUserId().getValue() : null,
                            authFlowToken.getEmail().getValue(),
                            AuthFlowType.PASSWORD_RESET,
                            authFlowToken.getToken(),
                            pendingPasswordHash))
                    .build();
        }

        OtpSession verifiedSession = otpService.verifyOtp(command.getOtpSessionId(), command.getOtpCode(),
                AuthFlowType.PASSWORD_RESET);

        if (!Objects.equals(verifiedSession.getReferenceToken(), command.getResetToken())) {
            throw new AppLogicException(ErrorCode.INVALID_AUTH_FLOW_TOKEN, command.getResetToken());
        }

        if (authFlowToken.getUserId() == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, command.getResetToken());
        }

        User user = userRepository.findById(authFlowToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        authFlowToken.getUserId().getValue()));

        String passwordHash = verifiedSession.getData() != null
                ? verifiedSession.getData()
                : passwordHashEncoder.encode(command.getNewPassword());

        // Reset succeeds only after proving account ownership via OTP, so clear any
        // login lockout at the same time - the guessed old password is moot now.
        userRepository.save(user.unlockLogin().toBuilder().passwordHash(passwordHash).build());
        authFlowLinkService.markUsed(authFlowToken);

        return ResetPasswordResultDto.builder()
                .passwordChanged(true)
                .build();
    }
}