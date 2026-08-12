package com.vetautet.app.presentation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.vetautet.app.presentation.config.ratelimit.RateLimitInterceptor;

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
}
