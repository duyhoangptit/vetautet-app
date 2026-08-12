package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.domain.auth.model.AuthFlowTokenStatus;
import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.infrastructure.persistence.jpa.entity.AuthFlowTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AuthFlowTokenJpaRepository extends JpaRepository<AuthFlowTokenJpaEntity, UUID> {

    Optional<AuthFlowTokenJpaEntity> findByToken(String token);

    @Modifying
    @Query("UPDATE AuthFlowTokenJpaEntity t SET t.status = com.vetautet.app.domain.auth.model.AuthFlowTokenStatus.INACTIVE " +
            "WHERE t.email = :email AND t.flowType = :flowType AND t.status = :status")
    void deactivateActiveTokens(@Param("email") String email,
                                @Param("flowType") AuthFlowType flowType,
                                @Param("status") AuthFlowTokenStatus status);
}