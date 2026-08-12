package com.vetautet.app.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
* JPA entity for RSA Key Pair table
* Infrastructure layer representation
*/
@Entity
@Table(name = "rsa_key_pairs", indexes = {
        @Index(name = "idx_rsa_key_pair_key_id", columnList = "key_id"),
        @Index(name = "idx_rsa_key_pair_user_id", columnList = "user_id"),
        @Index(name = "idx_rsa_key_pair_is_active", columnList = "is_active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RsaKeyPairJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "key_pair_id", nullable = false, updatable = false)
    private UUID keyPairId;

    @Column(name = "key_id", nullable = false, unique = true, length = 100)
    private String keyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "algorithm", nullable = false, length = 50)
    private String algorithm;

    @Column(name = "key_size", nullable = false)
    private Integer keySize;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}

