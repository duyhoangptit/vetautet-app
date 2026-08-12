package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.infrastructure.persistence.jpa.entity.UserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
* Spring Data JPA repository for UserJpaEntity
* Infrastructure layer - framework specific
*/
@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    /**
     * Find user by email
     */
    Optional<UserJpaEntity> findByEmail(String email);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users by status with pagination
     */
    Page<UserJpaEntity> findByStatus(String status, Pageable pageable);

    /**
     * Search users by keyword in name or email
     */
    @Query("SELECT u FROM UserJpaEntity u WHERE " +
            "LOWER(u.piiFirstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.piiLastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<UserJpaEntity> searchUsers(@Param("keyword") String keyword, Pageable pageable);

}
 