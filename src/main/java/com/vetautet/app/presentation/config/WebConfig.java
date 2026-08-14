package com.vetautet.app.presentation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.vetautet.app.presentation.config.ratelimit.RateLimitInterceptor;
import com.vetautet.app.shared.common.util.PageableSanitizer;

import lombok.RequiredArgsConstructor;

/**
 * Global Spring MVC configuration. Registers cross-cutting interceptors such
 * as the rate-limit module.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor);
    }

    /**
     * Caps {@code size} for any controller method that takes a {@link
     * org.springframework.data.domain.Pageable} parameter directly (Spring
     * resolves it from the {@code page}/{@code size}/{@code sort} request
     * params via {@code PageableHandlerMethodArgumentResolver}, which reads
     * this customizer). Reusing {@link PageableSanitizer#MAX_PAGE_SIZE} keeps
     * this in sync with the adapter-level cap.
     * <p>
     * <b>Note:</b> this resolver only runs when a handler method argument is
     * typed {@code Pageable}. {@code UserController} currently parses
     * {@code page}/{@code size}/{@code sortBy}/{@code sortDir} as separate
     * primitives and builds {@code PageRequest} itself, so this customizer
     * does <b>not</b> apply to it - that's why the real, enforced cap for
     * those endpoints lives in {@link PageableSanitizer}, called from the
     * repository adapters. This bean protects any endpoint written the
     * idiomatic Spring Data way (a bare {@code Pageable pageable} parameter),
     * present or future.
     */
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(PageableSanitizer.MAX_PAGE_SIZE);
            resolver.setFallbackPageable(PageRequest.of(0, 20));
        };
    }
}
