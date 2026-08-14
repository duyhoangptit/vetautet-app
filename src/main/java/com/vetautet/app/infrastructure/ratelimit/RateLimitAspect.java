package com.vetautet.app.infrastructure.ratelimit;

import java.lang.reflect.Method;
import java.time.Duration;

import com.vetautet.app.presentation.config.ratelimit.RateLimitProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.exception.RateLimitExceededException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Enforces per-key request throttling for methods annotated with {@link RateLimited}.
 * Backed by Redisson's distributed {@link RRateLimiter} so limits hold consistently
 * across all app instances sharing the same Redis.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedissonClient redissonClient;
    private final RateLimitProperties properties;

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(rateLimited)")
    public Object enforce(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String operation = resolveOperation(method, rateLimited);
        String key = resolveKey(method, joinPoint.getArgs(), rateLimited.key());
        String limiterKey = "ratelimit:" + operation + ":" + key;

        if (!properties.isEnabled()) {
            // e.g. app.rate-limit.enabled=false under the pentest profile, so
            // security testing tooling isn't throttled or IP-blocked.
            log.debug("Rate-limit disabled (app.rate-limit.enabled=false), allowing method={}", method);
            return true;
        }

        boolean withinLimit;
        try {
            RRateLimiter limiter = redissonClient.getRateLimiter(limiterKey);
            limiter.trySetRate(RateType.OVERALL, rateLimited.permits(), Duration.ofSeconds(rateLimited.periodSeconds()));
            withinLimit = limiter.tryAcquire();
        } catch (RedisException ex) {
            // Fail open: a Redis outage must not turn into a full outage of the
            // protected operation. See RedisConfig's CustomCacheErrorHandler for the
            // same fail-open philosophy applied to @Cacheable.
            log.warn("Redis unavailable, allowing request without rate-limit: operation={} key={} error={}",
                    operation, key, ex.getMessage());
            return joinPoint.proceed();
        }

        if (!withinLimit) {
            log.warn("Rate limit exceeded for operation={} key={}", operation, key);
            throw new RateLimitExceededException(ErrorCode.RATE_LIMIT_EXCEEDED, operation);
        }

        return joinPoint.proceed();
    }

    private String resolveOperation(Method method, RateLimited rateLimited) {
        if (StringUtils.hasText(rateLimited.operation())) {
            return rateLimited.operation();
        }

        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    private String resolveKey(Method method, Object[] args, String expression) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        for (int index = 0; index < args.length; index++) {
            context.setVariable("p" + index, args[index]);
            context.setVariable("a" + index, args[index]);
            if (parameterNames != null && index < parameterNames.length) {
                context.setVariable(parameterNames[index], args[index]);
            }
        }

        Object value = expressionParser.parseExpression(expression).getValue(context);
        if (value == null || !StringUtils.hasText(value.toString())) {
            throw new IllegalArgumentException("Resolved rate-limit key is blank");
        }

        return value.toString();
    }
}
