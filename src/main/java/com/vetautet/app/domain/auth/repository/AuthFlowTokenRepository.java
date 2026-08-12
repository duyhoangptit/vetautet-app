package com.vetautet.app.domain.auth.repository;

import com.vetautet.app.domain.auth.model.AuthFlowToken;
import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.domain.user.model.Email;

import java.util.Optional;

public interface AuthFlowTokenRepository {

    AuthFlowToken save(AuthFlowToken authFlowToken);

    Optional<AuthFlowToken> findByToken(String token);

    void deactivateActiveTokens(Email email, AuthFlowType flowType);
}