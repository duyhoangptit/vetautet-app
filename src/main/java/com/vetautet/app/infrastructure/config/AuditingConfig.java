package com.vetautet.app.infrastructure.config;

import com.vetautet.app.shared.common.util.AuthUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
* Auditing configuration
* Provides current user for JPA auditing
*/
@Configuration
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }

            // Handle other authentication types
            return Optional.of(AuthUtil.userId());
        };
    }
}
 