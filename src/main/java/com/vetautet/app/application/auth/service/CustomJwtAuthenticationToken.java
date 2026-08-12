package com.vetautet.app.application.auth.service;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Getter
public class CustomJwtAuthenticationToken extends AbstractAuthenticationToken {

    private final Jwt jwt;
    private final UUID userId;
    private final String portalCode;
    private final Set<String> roles;

    public CustomJwtAuthenticationToken(Jwt jwt, UUID userId, String portalCode) {
        super(Collections.emptyList());
        this.jwt = jwt;
        this.userId = userId;
        this.portalCode = portalCode;
        this.roles = Collections.emptySet();
        setAuthenticated(false);
    }

    public CustomJwtAuthenticationToken(Jwt jwt, UUID userId, String portalCode,
                                        Set<String> roles, Collection<? extends  GrantedAuthority> authorities) {
        super(authorities);
        this.jwt = jwt;
        this.userId = userId;
        this.portalCode = portalCode;
        this.roles = roles;
        setAuthenticated(true);
    }

    @Override
    public @Nullable Object getCredentials() {
        return this.jwt.getTokenValue();
    }

    @Override
    public @Nullable Object getPrincipal() {
        return this.jwt;
    }

}
