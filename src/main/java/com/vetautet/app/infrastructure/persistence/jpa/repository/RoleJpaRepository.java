package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.infrastructure.persistence.jpa.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Long> {

    @Query("""
        SELECT r.name
        FROM RoleJpaEntity r
        JOIN r.users u
        JOIN r.portal p
        WHERE p.code = :portalCode AND u.userId =:userId AND p.enabled = true
    """)
    List<String> findRoleNamesByUserIdAndPortal(UUID userId, String portalCode);
}
