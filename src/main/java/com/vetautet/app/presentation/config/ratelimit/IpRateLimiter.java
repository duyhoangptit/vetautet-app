package com.vetautet.app.presentation.config.ratelimit;

import java.time.Duration;
import java.time.Instant;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis-backed, per-scope-and-IP anti-abuse limiter. Enforces a two-tier
 * limit: exceeding a soft per-minute threshold requires captcha, exceeding a
 * hard per-hour threshold temporarily blocks the IP.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpRateLimiter {

    public static final String CAPTCHA_TOKEN_HEADER = "X-Captcha-Token";

    private final RedissonClient redissonClient;
    private final CaptchaVerifier captchaVerifier;

    public String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    public RateLimitDecision check(String scope, String ip, String captchaToken,
                                    int softLimitPerMinute, int hardLimitPerHour,
                                    long challengeTtlMinutes, long blockTtlMinutes) {
        try {
            return doCheck(scope, ip, captchaToken, softLimitPerMinute, hardLimitPerHour,
                    challengeTtlMinutes, blockTtlMinutes);
        } catch (RedisException ex) {
            // Fail open: a Redis outage must not turn into a full outage of the
            // protected endpoint. See RedisConfig's CustomCacheErrorHandler for the
            // same fail-open philosophy applied to @Cacheable.
            log.warn("Redis unavailable, allowing request without rate-limit: scope={} ip={} error={}",
                    scope, ip, ex.getMessage());
            return RateLimitDecision.allow();
        }
    }

    private RateLimitDecision doCheck(String scope, String ip, String captchaToken,
                                       int softLimitPerMinute, int hardLimitPerHour,
                                       long challengeTtlMinutes, long blockTtlMinutes) {
        String blockedKey = "blocked:" + scope + ":" + ip;
        String challengeKey = "challenge:" + scope + ":" + ip;

        if (isFlagged(blockedKey)) {
            return RateLimitDecision.block();
        }

        if (isFlagged(challengeKey)) {
            if (StringUtils.hasText(captchaToken) && captchaVerifier.verify(captchaToken, ip)) {
                clearFlag(challengeKey);
                resetMinuteCounter(scope, ip);
                return RateLimitDecision.allow();
            }
            return RateLimitDecision.challenge();
        }

        String minuteKey = "ratelimit:" + scope + ":minute:" + ip + ":" + minuteBucket();
        long minuteCount = incrementWithTtl(minuteKey, 90);
        if (minuteCount > softLimitPerMinute) {
            flag(challengeKey, challengeTtlMinutes);
            return RateLimitDecision.challenge();
        }

        String hourKey = "ratelimit:" + scope + ":hour:" + ip + ":" + hourBucket();
        long hourCount = incrementWithTtl(hourKey, 3700);
        if (hourCount > hardLimitPerHour) {
            flag(blockedKey, blockTtlMinutes);
            return RateLimitDecision.block();
        }

        return RateLimitDecision.allow();
    }

    private boolean isFlagged(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.isExists();
    }

    private void flag(String key, long ttlMinutes) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set("1", Duration.ofMinutes(ttlMinutes));
    }

    private void clearFlag(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.delete();
    }

    private void resetMinuteCounter(String scope, String ip) {
        String minuteKey = "ratelimit:" + scope + ":minute:" + ip + ":" + minuteBucket();
        redissonClient.getAtomicLong(minuteKey).delete();
    }

    private long incrementWithTtl(String key, long ttlSeconds) {
        RAtomicLong counter = redissonClient.getAtomicLong(key);
        long value = counter.incrementAndGet();
        if (value == 1) {
            counter.expire(Duration.ofSeconds(ttlSeconds));
        }
        return value;
    }

    private long minuteBucket() {
        return Instant.now().getEpochSecond() / 60;
    }

    private long hourBucket() {
        return Instant.now().getEpochSecond() / 3600;
    }
}
