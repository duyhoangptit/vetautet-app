package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.auth.model.AuthFlowTokenStatus;
import com.vetautet.app.domain.auth.model.AuthFlowType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_flow_tokens", indexes = {
        @Index(name = "idx_auth_flow_tokens_token", columnList = "token", unique = true),
        @Index(name = "idx_auth_flow_tokens_email_flow", columnList = "email,flow_type,status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuthFlowTokenJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "auth_flow_token_id", nullable = false, updatable = false)
    private UUID authFlowTokenId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "flow_type", nullable = false, length = 30)
    private AuthFlowType flowType;

    @Column(name = "token", nullable = false, length = 100, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuthFlowTokenStatus status;
}


