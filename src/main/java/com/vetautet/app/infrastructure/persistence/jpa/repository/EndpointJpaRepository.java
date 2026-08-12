package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.application.auth.dto.EndpointDto;
import com.vetautet.app.infrastructure.persistence.jpa.entity.EndpointJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EndpointJpaRepository extends JpaRepository<EndpointJpaEntity, Long> {

    @Query("""
        SELECT DISTINCT new com.vetautet.app.application.auth.dto.EndpointDto(e.httpMethod, e.urlPattern)
        FROM EndpointJpaEntity e
        JOIN e.permissions p
        JOIN p.roles r
        JOIN r.portal portal
        WHERE portal.code = :portalCode AND r.name IN :roleNames
    """)
    List<EndpointDto> findEndpointsByRolesAndPortal(
            @Param("portalCode") String portalCode,
            @Param("roleNames") Collection<String> roleNames
    );
}
