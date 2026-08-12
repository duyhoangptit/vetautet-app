package com.vetautet.app.application.auth.usecase;

import com.vetautet.app.application.auth.dto.ActivateAccountCommand;
import com.vetautet.app.application.auth.dto.ActivateAccountResultDto;
import com.vetautet.app.application.auth.port.input.ActivateAccountUseCase;
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
public class ActivateAccountUseCaseImpl implements ActivateAccountUseCase {

    private final AuthFlowLinkService authFlowLinkService;
    private final OtpService otpService;
    private final UserRepository userRepository;

    @Override
    @RateLimited(key = "#command.activationToken", operation = "auth-activate-account", permits = 10, periodSeconds = 900)
    public ActivateAccountResultDto execute(ActivateAccountCommand command) {
        AuthFlowToken authFlowToken = authFlowLinkService.validateActiveToken(
                command.getActivationToken(),
                AuthFlowType.REGISTER_ACTIVATION);

        if (command.getOtpSessionId() == null || !StringUtils.hasText(command.getOtpCode())) {
            return ActivateAccountResultDto.builder()
                    .activated(false)
                    .otpChallenge(otpService.sendOtp(
                            authFlowToken.getUserId() != null ? authFlowToken.getUserId().getValue() : null,
                            authFlowToken.getEmail().getValue(),
                            AuthFlowType.REGISTER_ACTIVATION,
                            authFlowToken.getToken(),
                            null))
                    .build();
        }

        OtpSession verifiedSession = otpService.verifyOtp(
                command.getOtpSessionId(),
                command.getOtpCode(),
                AuthFlowType.REGISTER_ACTIVATION);

        if (!Objects.equals(verifiedSession.getReferenceToken(), command.getActivationToken())) {
            throw new AppLogicException(ErrorCode.INVALID_AUTH_FLOW_TOKEN, command.getActivationToken());
        }

        if (authFlowToken.getUserId() == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, command.getActivationToken());
        }

        User user = userRepository.findById(authFlowToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        authFlowToken.getUserId().getValue()));

        User activatedUser = user.activate().completeOnboarding();
        User savedUser = userRepository.save(activatedUser);
        authFlowLinkService.markUsed(authFlowToken);

        return ActivateAccountResultDto.builder()
                .activated(true)
                .userId(savedUser.getUserId().getValue())
                .build();
    }
}