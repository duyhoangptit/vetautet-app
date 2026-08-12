package com.vetautet.app.infrastructure.notification;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.vetautet.app.application.auth.port.output.AuthNotificationSender;
import com.vetautet.app.application.notification.dto.SendNotificationCommand;
import com.vetautet.app.application.notification.port.input.SendNotificationUseCase;
import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.domain.notification.model.NotificationChannelCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingAuthNotificationSender implements AuthNotificationSender {

    private static final String TEMPLATE_OTP_VERIFICATION    = "OTP_VERIFICATION";
    private static final String TEMPLATE_REGISTER_ACTIVATION = "REGISTER_ACTIVATION";
    private static final String TEMPLATE_FORGOT_PASSWORD     = "FORGOT_PASSWORD";
    private static final String REF_TYPE_AUTH                = "AUTH_FLOW";

    private final SendNotificationUseCase sendNotificationUseCase;

    @Override
    public void sendOtp(String email, AuthFlowType flowType, String otpCode) {
        log.debug("Dispatching OTP notification for flow {} to {}", flowType, email);
        sendNotificationUseCase.execute(SendNotificationCommand.builder()
                .channelCode(NotificationChannelCode.EMAIL)
                .templateCode(TEMPLATE_OTP_VERIFICATION)
                .recipient(email)
                .referenceType(REF_TYPE_AUTH)
                .variables(Map.of(
                        "full_name", email,
                        "otp_code", otpCode,
                        "expires_minutes", "5"
                ))
                .build());
    }

    @Override
    public void sendAuthFlowLink(String email, AuthFlowType flowType, String link) {
        log.debug("Dispatching auth flow link notification for flow {} to {}", flowType, email);
        sendNotificationUseCase.execute(SendNotificationCommand.builder()
                .channelCode(NotificationChannelCode.EMAIL)
                .templateCode(resolveAuthFlowTemplate(flowType))
                .recipient(email)
                .referenceType(REF_TYPE_AUTH)
                .variables(buildAuthFlowVariables(flowType, email, link))
                .build());
    }

    private String resolveAuthFlowTemplate(AuthFlowType flowType) {
        return switch (flowType) {
            case REGISTER_ACTIVATION -> TEMPLATE_REGISTER_ACTIVATION;
            case PASSWORD_RESET -> TEMPLATE_FORGOT_PASSWORD;
            default -> TEMPLATE_FORGOT_PASSWORD;
        };
    }

    private Map<String, String> buildAuthFlowVariables(AuthFlowType flowType, String email, String link) {
        return switch (flowType) {
            case REGISTER_ACTIVATION -> Map.of(
                    "full_name", email,
                    "activation_link", link,
                    "expires_at", "15 ph\u00fat"
            );
            case PASSWORD_RESET -> Map.of(
                    "full_name", email,
                    "email", email,
                    "reset_code", link,
                    "expires_minutes", "30"
            );
            default -> Map.of(
                    "full_name", email,
                    "email", email,
                    "reset_code", link,
                    "expires_minutes", "30"
            );
        };
    }
}