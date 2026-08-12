package com.vetautet.app.domain.user.repository;

import com.vetautet.app.domain.user.model.Endpoint;

import java.util.Collection;
import java.util.List;

public interface EndpointRepository {
    List<Endpoint> findEndpointsByRolesAndPortal(String portalCode, Collection<String> roleNames);
}
