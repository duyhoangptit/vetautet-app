package com.vetautet.app.infrastructure.security;

import com.vetautet.app.application.auth.service.CustomJwtAuthenticationToken;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        UUID userId = UUID.fromString(Objects.requireNonNull(source.getSubject()));
        String portal = source.getClaimAsString("portal");
        List<String> roles = source.getClaimAsStringList("roles");
        if (roles == null) {
            roles = Collections.emptyList();
        }

        Set<String> rolesSet = Set.copyOf(roles);

        // TBU: convert role to authority
        // 4. Chuyển đổi danh sách Roles thành các GrantedAuthority chuẩn của Spring Security (ROLE_ADMIN, ROLE_USER...)
        Collection<GrantedAuthority> authorities = roles.stream()
                .map(roleName -> {
                    // Đảm bảo có tiền tố ROLE_ để khớp với cấu trúc hasRole() của Spring nếu cần dùng
                    String formattedRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                    return new SimpleGrantedAuthority(formattedRole);
                })
                .collect(Collectors.toList());

        return new CustomJwtAuthenticationToken(source, userId, portal, rolesSet, authorities);
    }
}
