package com.vetautet.app.domain.auth.repository;

import com.vetautet.app.domain.auth.model.KeyId;
import com.vetautet.app.domain.auth.model.RsaKeyPair;
import com.vetautet.app.domain.user.model.UserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
* Repository interface for RsaKeyPair domain
* Domain layer - port (will be implemented by infrastructure layer)
*/
public interface RsaKeyPairRepository {

    RsaKeyPair save(RsaKeyPair rsaKeyPair);

    Optional<RsaKeyPair> findByKeyId(KeyId keyId);

    Optional<RsaKeyPair> findActiveByKeyId(KeyId keyId);

    Optional<RsaKeyPair> findValidKeyById(KeyId keyId, LocalDateTime now);

    List<RsaKeyPair> findByUserId(UserId userId);

    List<RsaKeyPair> findActiveByUserId(UserId userId);

    void deactivateByKeyId(KeyId keyId, UserId userId);

    void deactivateAllByUserId(UserId userId);

    boolean existsByKeyId(KeyId keyId);

    long countByUserId(UserId userId);
}
 