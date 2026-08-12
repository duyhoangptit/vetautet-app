package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.domain.auth.model.OtpSessionStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.OtpSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OtpSessionJpaRepository extends JpaRepository<OtpSessionJpaEntity, UUID> {

    Optional<OtpSessionJpaEntity> findFirstByEmailAndFlowTypeAndStatusAndReferenceTokenOrderByCreatedDateDesc(
            String email,
            AuthFlowType flowType,
            OtpSessionStatus status,
            String referenceToken);

    Optional<OtpSessionJpaEntity> findFirstByEmailAndFlowTypeAndStatusAndReferenceTokenIsNullOrderByCreatedDateDesc(
            String email,
            AuthFlowType flowType,
            OtpSessionStatus status);

    @Modifying
    @Query("UPDATE OtpSessionJpaEntity o SET o.status = com.vetautet.app.domain.auth.model.OtpSessionStatus.INACTIVE " +
            "WHERE o.email = :email AND o.flowType = :flowType AND o.status = :status " +
            "AND ((:referenceToken IS NULL AND o.referenceToken IS NULL) OR o.referenceToken = :referenceToken)")
    void deactivateActiveSessions(@Param("email") String email,
                                  @Param("flowType") AuthFlowType flowType,
                                  @Param("referenceToken") String referenceToken,
                                  @Param("status") OtpSessionStatus status);
}