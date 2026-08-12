
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.domain.auth.model.OtpSessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_sessions", indexes = {
        @Index(name = "idx_otp_sessions_email_flow", columnList = "email,flow_type,status"),
        @Index(name = "idx_otp_sessions_reference", columnList = "reference_token"),
        @Index(name = "idx_otp_sessions_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSessionJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "otp_session_id", nullable = false, updatable = false)
    private UUID otpSessionId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "flow_type", nullable = false, length = 30)
    private AuthFlowType flowType;

    @Column(name = "reference_token", length = 100)
    private String referenceToken;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OtpSessionStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "data", length = 1000)
    private String data;
}