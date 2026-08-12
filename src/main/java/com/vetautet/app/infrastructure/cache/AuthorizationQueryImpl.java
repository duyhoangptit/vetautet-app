package com.vetautet.app.infrastructure.cache;

import com.vetautet.app.application.auth.dto.EndpointDto;
import com.vetautet.app.application.auth.port.output.AuthorizationQuery;
import com.vetautet.app.infrastructure.persistence.jpa.repository.EndpointJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationQueryImpl implements AuthorizationQuery {

    private final EndpointJpaRepository endpointJpaRepository;

    // Cache kết quả: Nếu cùng 1 nhóm Roles vào cùng 1 Portal, hệ thống lấy ngay từ Memory/Redis mất <1ms
    // sử dụng sorted để đảm bảo ko ảnh hưởng bởi thứ tự role trong list
    @Override
    @Cacheable(value = "portal-role-endpoints", key =  "#portalCode + ':' + T(java.lang.String).join(',', #roles.stream().sorted().toList())")
    public List<EndpointDto> getAllowedEndpoints(String portalCode, Collection<String> roles) {
        if (roles == null || roles.isEmpty()) return Collections.emptyList();
        return endpointJpaRepository.findEndpointsByRolesAndPortal(portalCode, roles);
    }
}
