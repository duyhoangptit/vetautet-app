package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.domain.auth.model.OtpSession;
import com.vetautet.app.domain.auth.model.OtpSessionStatus;
import com.vetautet.app.domain.auth.repository.OtpSessionRepository;
import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.infrastructure.persistence.jpa.repository.OtpSessionJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.OtpSessionEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class OtpSessionRepositoryAdapter implements OtpSessionRepository {

    private final OtpSessionJpaRepository jpaRepository;
    private final OtpSessionEntityMapper mapper;

    @Override
    public OtpSession save(OtpSession otpSession) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(otpSession)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OtpSession> findById(UUID otpSessionId) {
        return jpaRepository.findById(otpSessionId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OtpSession> findLatestActiveSession(Email email, AuthFlowType flowType, String referenceToken) {
        Optional<OtpSession> latestSession;
        if (referenceToken == null) {
            latestSession = jpaRepository
                    .findFirstByEmailAndFlowTypeAndStatusAndReferenceTokenIsNullOrderByCreatedDateDesc(
                            email.getValue(),
                            flowType,
                            OtpSessionStatus.ACTIVE)
                    .map(mapper::toDomain);
        } else {
            latestSession = jpaRepository
                    .findFirstByEmailAndFlowTypeAndStatusAndReferenceTokenOrderByCreatedDateDesc(
                            email.getValue(),
                            flowType,
                            OtpSessionStatus.ACTIVE,
                            referenceToken)
                    .map(mapper::toDomain);
        }

        return latestSession;
    }

    @Override
    public void deactivateActiveSessions(Email email, AuthFlowType flowType, String referenceToken) {
        jpaRepository.deactivateActiveSessions(email.getValue(), flowType, referenceToken, OtpSessionStatus.ACTIVE);
    }
}