package com.vetautet.app.presentation.config.ratelimit;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Single global MVC enforcement point for the rate-limit module. Runs before
 * request body parsing, validation, and controller execution; only methods
 * annotated with {@link RateLimit} are affected even though this interceptor
 * is registered globally.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final IpRateLimiter rateLimiter;
    private final RateLimitResponseWriter responseWriter;
    private final RateLimitProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        if (!properties.isEnabled()) {
            // e.g. app.rate-limit.enabled=false under the pentest profile, so
            // security testing tooling isn't throttled or IP-blocked.
            log.debug("Rate-limit module disabled (app.rate-limit.enabled=false), allowing request uri={}",
                    request.getRequestURI());
            return true;
        }

        String scopeKey = rateLimit.scope() == null ? handlerMethod.getMethod().getName() : rateLimit.scope();
        RateLimitProperties.RateLimitSpec spec = properties.getScopes().get(scopeKey);
        if (spec == null) {
            log.warn("@RateLimit scope '{}' has no matching app.rate-limit.scopes entry, allowing request", scopeKey);
            return true;
        }

        String clientIp = rateLimiter.resolveClientIp(request);
        String captchaToken = request.getHeader(IpRateLimiter.CAPTCHA_TOKEN_HEADER);

        RateLimitDecision decision = rateLimiter.check(
                rateLimit.scope(),
                clientIp,
                captchaToken,
                spec.getSoftLimitPerMinute(),
                spec.getHardLimitPerHour(),
                spec.getChallengeTtlMinutes(),
                spec.getBlockTtlMinutes());

        if (!decision.allowed()) {
            log.warn("Rate limit rejected request uri={} ip={} scope={} requiresCaptcha={} blocked={}",
                    request.getRequestURI(), clientIp, rateLimit.scope(),
                    decision.requiresCaptcha(), decision.blocked());
            responseWriter.writeTooManyRequests(response, request, decision);
            return false;
        }

        return true;
    }
}
