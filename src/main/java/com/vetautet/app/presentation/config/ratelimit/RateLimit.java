package com.vetautet.app.presentation.config.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as protected by the rate-limit module.
 * {@link #scope()} matches an entry under {@code app.rate-limit.scopes} in
 * configuration; enforcement is done by a single global
 * {@link RateLimitInterceptor}, not by per-endpoint wiring.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String scope() default "";
}
