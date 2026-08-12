package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.auth.model.KeyId;
import com.vetautet.app.domain.auth.model.RsaKeyPair;
import com.vetautet.app.domain.auth.repository.RsaKeyPairRepository;
import com.vetautet.app.domain.user.model.UserId;
import com.vetautet.app.infrastructure.persistence.jpa.entity.RsaKeyPairJpaEntity;
import com.vetautet.app.infrastructure.persistence.jpa.repository.RsaKeyPairJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.RsaKeyPairEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing the domain RsaKeyPairRepository interface
 * Bridges domain layer with infrastructure (JPA)
 * This is the hexagonal architecture adapter pattern
 */
@Component
@RequiredArgsConstructor
@Transactional
public class RsaKeyPairRepositoryAdapter implements RsaKeyPairRepository {

    private final RsaKeyPairJpaRepository jpaRepository;
    private final RsaKeyPairEntityMapper mapper;

    @Override
    public RsaKeyPair save(RsaKeyPair rsaKeyPair) {
        RsaKeyPairJpaEntity entity = mapper.toEntity(rsaKeyPair);
        RsaKeyPairJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RsaKeyPair> findByKeyId(KeyId keyId) {
        return jpaRepository.findByKeyId(keyId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RsaKeyPair> findActiveByKeyId(KeyId keyId) {
        return jpaRepository.findByKeyIdAndIsActiveTrue(keyId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RsaKeyPair> findValidKeyById(KeyId keyId, LocalDateTime now) {
        return jpaRepository.findValidKeyPair(keyId.getValue(), now)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RsaKeyPair> findByUserId(UserId userId) {
        return jpaRepository.findByUserId(userId.getValue())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RsaKeyPair> findActiveByUserId(UserId userId) {
        return jpaRepository.findByUserIdAndIsActiveTrue(userId.getValue())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deactivateByKeyId(KeyId keyId, UserId userId) {
        jpaRepository.deactivateByKeyIdAndUserId(keyId.getValue(), userId.getValue());
    }

    @Override
    public void deactivateAllByUserId(UserId userId) {
        jpaRepository.deactivateAllByUserId(userId.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByKeyId(KeyId keyId) {
        return jpaRepository.existsByKeyId(keyId.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserId(UserId userId) {
        return jpaRepository.countByUserId(userId.getValue());
    }
}
 