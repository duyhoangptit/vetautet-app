package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.application.auth.port.output.RoleRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository roleJpaRepository;

    @Override
    public List<String> findRoleNamesByUserIdAndPortalCode(UUID userId, String portalCode) {
        return roleJpaRepository.findRoleNamesByUserIdAndPortal(userId, portalCode);
    }
}
