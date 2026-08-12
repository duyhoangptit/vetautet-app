package com.vetautet.app.presentation.config.ratelimit;

/**
 * Outcome of an {@link IpRateLimiter#check(String, String, String, int, int, long, long)}
 * evaluation.
 */
public record RateLimitDecision(boolean allowed, boolean requiresCaptcha, boolean blocked) {

    public static RateLimitDecision allow() {
        return new RateLimitDecision(true, false, false);
    }

    public static RateLimitDecision challenge() {
        return new RateLimitDecision(false, true, false);
    }

    public static RateLimitDecision block() {
        return new RateLimitDecision(false, false, true);
    }
}
