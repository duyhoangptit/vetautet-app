package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.infrastructure.persistence.jpa.entity.RsaKeyPairJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
* Spring Data JPA repository for RsaKeyPairJpaEntity
* Infrastructure layer - framework specific
*/
@Repository
public interface RsaKeyPairJpaRepository extends JpaRepository<RsaKeyPairJpaEntity, UUID> {

    /**
     * Find by key ID
     */
    Optional<RsaKeyPairJpaEntity> findByKeyId(String keyId);

    /**
     * Find active key pair by keyId
     */
    Optional<RsaKeyPairJpaEntity> findByKeyIdAndIsActiveTrue(String keyId);

    /**
     * Find active and non-expired key pair by keyId
     */
    @Query("SELECT k FROM RsaKeyPairJpaEntity k WHERE k.keyId = :keyId " +
            "AND k.isActive = true " +
            "AND (k.expiresAt IS NULL OR k.expiresAt > :now)")
    Optional<RsaKeyPairJpaEntity> findValidKeyPair(
            @Param("keyId") String keyId,
            @Param("now") LocalDateTime now);

    /**
     * Find all key pairs by user ID
     */
    List<RsaKeyPairJpaEntity> findByUserId(UUID userId);

    /**
     * Find active key pairs by user ID
     */
    List<RsaKeyPairJpaEntity> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Deactivate all keys for a user
     */
    @Modifying
    @Query("UPDATE RsaKeyPairJpaEntity k SET k.isActive = false WHERE k.keyId = :keyId AND k.userId = :userId")
    int deactivateByKeyIdAndUserId(@Param("keyId") String keyId, @Param("userId") UUID userId);

    /**
     * Deactivate all keys for a user
     */
    @Modifying
    @Query("UPDATE RsaKeyPairJpaEntity k SET k.isActive = false WHERE k.userId = :userId")
    void deactivateAllByUserId(@Param("userId") UUID userId);

    /**
     * Check if key ID exists
     */
    boolean existsByKeyId(String keyId);

    /**
     * Count keys by user ID
     */
    long countByUserId(UUID userId);
}
 