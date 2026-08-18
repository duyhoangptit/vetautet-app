package com.vetautet.app.presentation.config.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * In-process, per-instance fallback for {@link IpRateLimiter}, used only when
 * the distributed Redisson limiter is unreachable.
 *
 * <p>Before this class existed, a Redis outage made {@link IpRateLimiter}
 * fail open (see git history) - meaning the exact moment Redis goes down,
 * every rate-limited public endpoint (departure search, availability check,
 * etc.) loses its only DB-load protection at once. That's the failure mode
 * this class closes: instead of "no limiting", a Redis outage degrades to
 * "limiting enforced per app instance instead of globally".
 *
 * <p><b>Trade-off, by design:</b> counters here live in this JVM's heap only,
 * not shared across instances. Under a Redis outage with N app instances,
 * the effective ceiling for one IP becomes roughly {@code softLimit * N}
 * instead of a true cluster-wide {@code softLimit}. That is an accepted,
 * intentional degradation - still far better than fully open, and it
 * self-heals the instant Redis is reachable again since {@link IpRateLimiter}
 * always attempts Redis first and only falls back on {@code RedisException}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalFallbackRateLimiter {

    private final CaptchaVerifier captchaVerifier;

    /** Per-scope-per-IP request counts for the current minute bucket. */
    private final Cache<String, AtomicLong> minuteCounters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2))
            .maximumSize(50_000)
            .build();

    /** Per-scope-per-IP request counts for the current hour bucket. */
    private final Cache<String, AtomicLong> hourCounters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(70))
            .maximumSize(50_000)
            .build();

    /** IPs currently required to solve a captcha, mapped to when that requirement expires. */
    private final Cache<String, Instant> challengeFlags = Caffeine.newBuilder()
            .expireAfter(new ExpiresAt())
            .maximumSize(50_000)
            .build();

    /** IPs currently blocked outright, mapped to when the block expires. */
    private final Cache<String, Instant> blockedFlags = Caffeine.newBuilder()
            .expireAfter(new ExpiresAt())
            .maximumSize(50_000)
            .build();

    /**
     * Mirrors {@link IpRateLimiter}'s two-tier decision logic (soft
     * per-minute limit -> captcha challenge, hard per-hour limit -> block),
     * entirely in-memory. Intentionally has no failure path of its own -
     * this method is already the fallback.
     */
    public RateLimitDecision check(String scope, String ip, String captchaToken,
                                    int softLimitPerMinute, int hardLimitPerHour,
                                    long challengeTtlMinutes, long blockTtlMinutes) {
        String blockedKey = "blocked:" + scope + ":" + ip;
        String challengeKey = "challenge:" + scope + ":" + ip;

        if (blockedFlags.getIfPresent(blockedKey) != null) {
            return RateLimitDecision.block();
        }

        if (challengeFlags.getIfPresent(challengeKey) != null) {
            if (StringUtils.hasText(captchaToken) && captchaVerifier.verify(captchaToken, ip)) {
                challengeFlags.invalidate(challengeKey);
                resetMinuteCounter(scope, ip);
                return RateLimitDecision.allow();
            }
            return RateLimitDecision.challenge();
        }

        long minuteCount = increment(minuteCounters, minuteKey(scope, ip));
        if (minuteCount > softLimitPerMinute) {
            challengeFlags.put(challengeKey, Instant.now().plus(Duration.ofMinutes(challengeTtlMinutes)));
            return RateLimitDecision.challenge();
        }

        long hourCount = increment(hourCounters, hourKey(scope, ip));
        if (hourCount > hardLimitPerHour) {
            blockedFlags.put(blockedKey, Instant.now().plus(Duration.ofMinutes(blockTtlMinutes)));
            return RateLimitDecision.block();
        }

        return RateLimitDecision.allow();
    }

    private long increment(Cache<String, AtomicLong> counters, String key) {
        return counters.get(key, k -> new AtomicLong()).incrementAndGet();
    }

    private void resetMinuteCounter(String scope, String ip) {
        minuteCounters.invalidate(minuteKey(scope, ip));
    }

    private String minuteKey(String scope, String ip) {
        return "local:" + scope + ":minute:" + ip + ":" + (Instant.now().getEpochSecond() / 60);
    }

    private String hourKey(String scope, String ip) {
        return "local:" + scope + ":hour:" + ip + ":" + (Instant.now().getEpochSecond() / 3600);
    }

    /**
     * Expires a cache entry at the wall-clock {@link Instant} stored as its
     * value, computed once at write time. Reads don't extend the TTL - a
     * challenge/block window is a fixed wall-clock deadline, not an
     * idle timeout.
     */
    private static final class ExpiresAt implements Expiry<String, Instant> {
        @Override
        public long expireAfterCreate(String key, Instant expiresAt, long currentTime) {
            return remainingNanos(expiresAt);
        }

        @Override
        public long expireAfterUpdate(String key, Instant expiresAt, long currentTime, long currentDuration) {
            return remainingNanos(expiresAt);
        }

        @Override
        public long expireAfterRead(String key, Instant expiresAt, long currentTime, long currentDuration) {
            return currentDuration;
        }

        private long remainingNanos(Instant expiresAt) {
            return Math.max(0, Duration.between(Instant.now(), expiresAt).toNanos());
        }
    }
}
