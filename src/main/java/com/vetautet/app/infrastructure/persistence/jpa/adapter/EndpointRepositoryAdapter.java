package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.user.model.Endpoint;
import com.vetautet.app.domain.user.repository.EndpointRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.EndpointJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.EndpointEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EndpointRepositoryAdapter implements EndpointRepository {

    private final EndpointJpaRepository endpointJpaRepository;
    private final EndpointEntityMapper endpointEntityMapper;

    @Override
    public List<Endpoint> findEndpointsByRolesAndPortal(String portalCode, Collection<String> roleNames) {
        return endpointJpaRepository.findEndpointsByRolesAndPortal(portalCode, roleNames)
                .stream()
                .map(endpointEntityMapper::toDomain)
                .toList();
    }
}
