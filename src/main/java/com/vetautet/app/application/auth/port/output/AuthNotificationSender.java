package com.vetautet.app.application.auth.port.output;

import com.vetautet.app.domain.auth.model.AuthFlowType;

public interface AuthNotificationSender {

    void sendOtp(String email, AuthFlowType flowType, String otpCode);

    void sendAuthFlowLink(String email, AuthFlowType flowType, String link);
}
