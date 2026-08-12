package com.vetautet.app.shared.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

public class AuthUtil {

    private AuthUtil () {}

    public static Jwt getAuthPayload() {
        Object obj = Objects.requireNonNull(getAuthentication()).getPrincipal();

        if (obj == null) {
            return null;
        }
        return (Jwt) obj;
    }

    public static String userId() {
        try {
            // Handle JWT authentication
            if (getAuthentication().getPrincipal() instanceof Jwt) {
                return getAuthPayload().getClaimAsString("sub");
            }

            // Handle other authentication types
            return getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }

    }

    public static String keyId() {
        return getAuthPayload().getClaimAsString("kid");
    }

    public static String portal() {
        return getAuthPayload().getClaimAsString("portal");
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}

