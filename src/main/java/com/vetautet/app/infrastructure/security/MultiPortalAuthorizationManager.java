package com.vetautet.app.infrastructure.security;

import com.vetautet.app.application.auth.dto.EndpointDto;
import com.vetautet.app.application.auth.port.output.AuthorizationQuery;
import com.vetautet.app.application.auth.service.CustomJwtAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class MultiPortalAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final AuthorizationQuery authService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public void verify(Supplier<? extends @Nullable Authentication> authenticationSupplier, RequestAuthorizationContext object) {
        authorize(authenticationSupplier, object);
    }

    @Override
    public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authenticationSupplier, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        String currentMethod = request.getMethod();
        String currentPath = request.getRequestURI();

        Authentication auth = authenticationSupplier.get();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return new AuthorizationDecision(false);
        }

        // 1. Giả định bạn đã custom JwtAuthenticationToken để chứa các thông tin này từ JWT
        CustomJwtAuthenticationToken jwtAuth = (CustomJwtAuthenticationToken) auth;
        String portalCode = jwtAuth.getPortalCode();
        Collection<String> userRoles = jwtAuth.getRoles();

        // 2. Lấy nhanh danh sách API được cho phép từ Cache
        List<EndpointDto> allowedEndpoints = authService.getAllowedEndpoints(portalCode, userRoles);

        // 3. Khớp chuỗi URL & Method xem có Record nào matching không
        for (EndpointDto endpoint : allowedEndpoints) {
            boolean methodMatches = endpoint.httpMethod().equals("*")
                    || endpoint.httpMethod().equalsIgnoreCase(currentMethod);

            boolean pathMatches = pathMatcher.match(endpoint.urlPattern(), currentPath);

            if (methodMatches && pathMatches) {
                return new AuthorizationDecision(true); // Khớp quyền -> Cho qua
            }
        }

        return new AuthorizationDecision(false); // Kháng nghị từ chối truy cập (403 Forbidden)
    }
}

