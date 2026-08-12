
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
* JPA entity for User table
* This is the infrastructure layer representation
*/
@Entity
@Table(name = "users")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "pii_first_name", nullable = false, length = 100)
    private String piiFirstName;

    @Column(name = "pii_last_name", nullable = false, length = 100)
    private String piiLastName;

    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "mobile_country_code", length = 10)
    private String mobileCountryCode;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(name = "avatar_url", length = 100)
    private String avatarUrl;

    @Column(name = "status", nullable = false, length = 3)
    private String status;

    @Column(name = "is_onboarding", nullable = false)
    private Boolean isOnboarding;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;
}
 