package com.vetautet.app.application.auth.usecase;

import com.vetautet.app.application.auth.dto.AuthFlowLinkDto;
import com.vetautet.app.application.auth.dto.ForgotPasswordCommand;
import com.vetautet.app.application.auth.port.input.ForgotPasswordUseCase;
import com.vetautet.app.application.auth.service.AuthFlowLinkService;
import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.domain.user.model.User;
import com.vetautet.app.domain.user.repository.UserRepository;
import com.vetautet.app.infrastructure.ratelimit.RateLimited;
import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ForgotPasswordUseCaseImpl implements ForgotPasswordUseCase {

    private final UserRepository userRepository;
    private final AuthFlowLinkService authFlowLinkService;

    @Override
    @RateLimited(key = "#command.email.toLowerCase()", operation = "auth-forgot-password", permits = 3, periodSeconds = 900)
    public AuthFlowLinkDto execute(ForgotPasswordCommand command) {
        User user = userRepository.findByEmail(Email.of(command.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND_BY_EMAIL, command.getEmail()));

        if (!user.isActive()) {
            throw new AppLogicException(ErrorCode.INVALID_USER_STATE, command.getEmail());
        }

        return authFlowLinkService.issueLink(user, AuthFlowType.PASSWORD_RESET);
    }
}