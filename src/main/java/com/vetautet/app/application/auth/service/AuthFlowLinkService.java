package com.vetautet.app.application.auth.service;

import com.vetautet.app.application.auth.dto.AuthFlowLinkDto;
import com.vetautet.app.application.auth.port.output.AuthNotificationSender;
import com.vetautet.app.domain.auth.model.AuthFlowToken;
import com.vetautet.app.domain.auth.model.AuthFlowTokenStatus;
import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.domain.auth.repository.AuthFlowTokenRepository;
import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.domain.user.model.User;
import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
* Shared service for issuing and validating auth flow entry links.
*/
@Service
@RequiredArgsConstructor
@Transactional
public class AuthFlowLinkService {

    private final AuthFlowTokenRepository authFlowTokenRepository;
    private final AuthNotificationSender notificationSender;

    @Value("${app.auth.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.auth.link-expiration-minutes:15}")
    private long linkExpirationMinutes;

    public AuthFlowLinkDto issueLink(User user, AuthFlowType flowType) {
        return issueLink(user.getUserId() != null ? user.getUserId().getValue() : null, user.getEmail().getValue(), flowType);
    }

    public AuthFlowLinkDto issueLink(UUID userId, String emailAddress, AuthFlowType flowType) {
        Email email = Email.of(emailAddress);
        return issueLinkInternal(userId, email, flowType);
    }

    private AuthFlowLinkDto issueLinkInternal(UUID userId, Email email, AuthFlowType flowType) {
        authFlowTokenRepository.deactivateActiveTokens(email, flowType);

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(linkExpirationMinutes * 60);

        AuthFlowToken authFlowToken = AuthFlowToken.builder()
                .authFlowTokenId(UUID.randomUUID())
                .userId(userId != null ? com.vetautet.app.domain.user.model.UserId.of(userId) : null)
                .email(email)
                .flowType(flowType)
                .token(token)
                .expiresAt(expiresAt)
                .status(AuthFlowTokenStatus.ACTIVE)
                .build();

        authFlowTokenRepository.save(authFlowToken);

        String link = buildLink(flowType, token);
        notificationSender.sendAuthFlowLink(email.getValue(), flowType, link);

        return AuthFlowLinkDto.builder()
                .token(token)
                .link(link)
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional(readOnly = true)
    public AuthFlowToken validateActiveToken(String token, AuthFlowType flowType) {
        AuthFlowToken authFlowToken = authFlowTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.AUTH_FLOW_TOKEN_NOT_FOUND, token));

        if (authFlowToken.getFlowType() != flowType || authFlowToken.getStatus() != AuthFlowTokenStatus.ACTIVE) {
            throw new AppLogicException(ErrorCode.INVALID_AUTH_FLOW_TOKEN, token);
        }

        if (!authFlowToken.isActiveAt(Instant.now())) {
            throw new AppLogicException(ErrorCode.AUTH_FLOW_TOKEN_EXPIRED, token);
        }

        return authFlowToken;
    }

    public void markUsed(AuthFlowToken authFlowToken) {
        authFlowTokenRepository.save(authFlowToken.consume(Instant.now()));
    }

    private String buildLink(AuthFlowType flowType, String token) {
        return switch (flowType) {
            case REGISTER_ACTIVATION -> frontendBaseUrl + "/activate-account?token=" + token;
            case PASSWORD_RESET -> frontendBaseUrl + "/reset-password?token=" + token;
            default -> frontendBaseUrl + "/auth-flow?token=" + token;
        };
    }
}