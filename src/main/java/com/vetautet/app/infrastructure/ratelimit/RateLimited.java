package com.vetautet.app.infrastructure.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.intellij.lang.annotations.Language;

/**
 * Throttles calls to the annotated method per resolved key, backed by a distributed
 * Redisson rate limiter. Use this to protect account- or token-scoped operations
 * (e.g. OTP/login/registration flows) from brute-force or spam abuse.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /**
     * SpEL expression used to derive the rate-limit key.
     * Example: #command.email.toLowerCase()
     */
    @Language("SpEL") String key();

    /**
     * Logical operation name used as part of the limiter key.
     * Defaults to "DeclaringClass.methodName" when blank.
     */
    String operation() default "";

    /**
     * Maximum number of calls allowed per key within {@link #periodSeconds()}.
     */
    int permits() default 5;

    /**
     * Sliding window size, in seconds, over which {@link #permits()} apply.
     */
    long periodSeconds() default 60;
}
