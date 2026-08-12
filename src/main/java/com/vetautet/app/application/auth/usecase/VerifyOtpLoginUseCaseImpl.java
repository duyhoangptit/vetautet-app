package com.vetautet.app.application.auth.usecase;

import com.vetautet.app.application.auth.dto.TokenDto;
import com.vetautet.app.application.auth.dto.VerifyOtpLoginCommand;
import com.vetautet.app.application.auth.port.input.VerifyOtpLoginUseCase;
import com.vetautet.app.application.auth.port.output.RoleRepository;
import com.vetautet.app.application.auth.service.AuthTokenIssueService;
import com.vetautet.app.application.auth.service.OtpService;
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

@Service
@RequiredArgsConstructor
@Transactional
public class VerifyOtpLoginUseCaseImpl implements VerifyOtpLoginUseCase {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenIssueService authTokenIssueService;

    @Override
    @RateLimited(key = "#command.otpSessionId", operation = "auth-verify-otp-login", permits = 10, periodSeconds = 300)
    public TokenDto execute(VerifyOtpLoginCommand command) {
        OtpSession verifiedSession = otpService.verifyOtp(
                command.getOtpSessionId(),
                command.getOtpCode(),
                AuthFlowType.LOGIN_2FA);

        User user = verifiedSession.getUserId() != null
                ? userRepository.findById(verifiedSession.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        verifiedSession.getUserId().getValue()))
                : userRepository.findByEmail(verifiedSession.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND_BY_EMAIL,
                        verifiedSession.getEmail().getValue()));

        if (!user.isActive()) {
            throw new AppLogicException(ErrorCode.INVALID_USER_STATE, user.getEmail().getValue());
        }

        String portal = verifiedSession.getData();
        var roles = roleRepository.findRoleNamesByUserIdAndPortalCode(user.getUserId().getValue(), portal);
        User userHasRole = user.updateRoles(roles);

        return authTokenIssueService.issueToken(userHasRole, portal);
    }
}