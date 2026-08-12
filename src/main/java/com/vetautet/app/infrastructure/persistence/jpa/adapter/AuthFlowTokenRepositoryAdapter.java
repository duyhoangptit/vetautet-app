package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.auth.model.AuthFlowToken;
import com.vetautet.app.domain.auth.model.AuthFlowTokenStatus;
import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.domain.auth.repository.AuthFlowTokenRepository;
import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.infrastructure.persistence.jpa.repository.AuthFlowTokenJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.AuthFlowTokenEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional
public class AuthFlowTokenRepositoryAdapter implements AuthFlowTokenRepository {

    private final AuthFlowTokenJpaRepository jpaRepository;
    private final AuthFlowTokenEntityMapper mapper;

    @Override
    public AuthFlowToken save(AuthFlowToken authFlowToken) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(authFlowToken)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthFlowToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(mapper::toDomain);
    }

    @Override
    public void deactivateActiveTokens(Email email, AuthFlowType flowType) {
        jpaRepository.deactivateActiveTokens(email.getValue(), flowType, AuthFlowTokenStatus.ACTIVE);
    }
}