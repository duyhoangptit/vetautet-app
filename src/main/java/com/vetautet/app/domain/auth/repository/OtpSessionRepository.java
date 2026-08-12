package com.vetautet.app.domain.auth.repository;

import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.domain.auth.model.OtpSession;
import com.vetautet.app.domain.user.model.Email;

import java.util.Optional;
import java.util.UUID;

public interface OtpSessionRepository {

    OtpSession save(OtpSession otpSession);

    Optional<OtpSession> findById(UUID otpSessionId);

    Optional<OtpSession> findLatestActiveSession(Email email, AuthFlowType flowType, String referenceToken);

    void deactivateActiveSessions(Email email, AuthFlowType flowType, String referenceToken);
}