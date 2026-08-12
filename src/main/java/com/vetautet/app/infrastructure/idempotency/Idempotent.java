package com.vetautet.app.infrastructure.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.intellij.lang.annotations.Language;

/**
* Marks a controller method as idempotent.
* The key supports SpEL expressions against controller arguments.
*/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * SpEL expression used to derive the idempotency key.
     * Example: #request.email.toLowerCase()
     */
    @Language("SpEL") String key();

    /**
     * Logical operation name used as part of the persisted uniqueness key.
     */
    String operation() default "";

    /**
     * Retention period for a successful idempotency record.
     */
    long ttlHours() default 24;

    /**
     * SpEL expressions to include in the request hash.
     * When empty (default), all hashable arguments are included.
     * Example: hashFields = {"#request"} or hashFields = {"#request.amount", "#request.currency"}
     */
    @Language("SpEL") String[] hashFields() default {};
}