package com.vetautet.app.presentation.rest.filter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import com.vetautet.app.shared.common.exception.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Coarse per-IP throttle for the login flow (credential submission and OTP
 * verification). Runs before Spring Security so abusive traffic is rejected as
 * early as possible. This is a first line of defense; per-account throttling
 * (email/token scoped) is enforced separately at the use-case layer via
 * {@code @RateLimited}.
 * <p>
 * Registration, activation, and password-reset flows are protected instead by
 * the {@code com.vetautet.app.presentation.config.ratelimit} module
 * ({@code @RateLimit}), which adds a captcha-challenge tier per endpoint scope.
 * Login and OTP verification stay on this simpler sliding-window filter because
 * a captcha-challenge lockout on failed-login attempts would be too disruptive
 * without a real captcha provider wired in yet.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final int PERMITS = 30;
    private static final Duration PERIOD = Duration.ofMinutes(1);

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/verify-otp-login");

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!PROTECTED_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);

        boolean withinLimit;
        try {
            RRateLimiter limiter = redissonClient.getRateLimiter("ratelimit:ip:auth:" + clientIp);
            limiter.trySetRate(RateType.OVERALL, PERMITS, PERIOD);
            withinLimit = limiter.tryAcquire();
        } catch (RedisException ex) {
            // Fail open: a Redis outage must not turn into a full outage of the login
            // flow. See RedisConfig's CustomCacheErrorHandler for the same fail-open
            // philosophy applied to @Cacheable.
            log.warn("Redis unavailable, allowing request without IP rate-limit: ip={} path={} error={}",
                    clientIp, request.getRequestURI(), ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (!withinLimit) {
            log.warn("IP rate limit exceeded for auth endpoints: ip={} path={}", clientIp, request.getRequestURI());
            writeTooManyRequests(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");

        BaseResponse<Void> body = BaseResponse.error(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                ErrorCode.RATE_LIMIT_EXCEEDED.getCode(),
                "Too many requests from this IP, please try again later",
                request.getRequestURI());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
