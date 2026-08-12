package com.vetautet.app.shared.common.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Holds the current request id so it can cross HTTP, executor and messaging boundaries.
 */
public final class RequestIdContext {

    public static final String HTTP_HEADER_NAME = "X-Request-Id";
    public static final String MESSAGE_HEADER_NAME = "requestId";
    public static final String REQUEST_ATTRIBUTE_NAME = RequestIdContext.class.getName() + ".REQUEST_ID";

    private static final String CONTENT_TYPE_HEADER_NAME = "contentType";
    private static final String EVENT_TYPE_HEADER_NAME = "eventType";
    private static final String APPLICATION_JSON = "application/json";
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String MDC_KEY = MESSAGE_HEADER_NAME;
    private static final ThreadLocal<String> CURRENT_REQUEST_ID = new ThreadLocal<>();

    private RequestIdContext() {
    }

    public static Optional<String> getCurrentRequestId() {
        return Optional.ofNullable(CURRENT_REQUEST_ID.get());
    }

    public static String resolveRequestId(String candidateRequestId) {
        if (candidateRequestId == null || candidateRequestId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return candidateRequestId.trim();
    }

    public static Scope open(String requestId) {
        String previousRequestId = CURRENT_REQUEST_ID.get();
        bind(requestId);
        return () -> bind(previousRequestId);
    }

    public static Snapshot snapshot() {
        return new Snapshot(CURRENT_REQUEST_ID.get());
    }

    public static void runWithRequestId(String requestId, Runnable runnable) {
        try (Scope scope = open(requestId)) {
            scope.hashCode();
            runnable.run();
        }
    }

    public static Map<String, Object> createMessageHeaders(String eventType) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(CONTENT_TYPE_HEADER_NAME, APPLICATION_JSON);
        headers.put(EVENT_TYPE_HEADER_NAME, eventType);
        // add request id into headers
        getCurrentRequestId().ifPresent(requestId -> headers.put(MESSAGE_HEADER_NAME, requestId));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // add authorization token into headers
            headers.put(AUTHORIZATION_HEADER_NAME, "Bearer " + jwt.getTokenValue());
        }
        return headers;
    }

    public static String readRequestId(Map<String, ?> headers) {
        if (headers == null) {
            return null;
        }

        Object requestId = headers.get(MESSAGE_HEADER_NAME);
        if (requestId instanceof String value && !value.isBlank()) {
            return value;
        }

        return null;
    }

    private static void bind(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            CURRENT_REQUEST_ID.remove();
            MDC.remove(MDC_KEY);
            return;
        }

        CURRENT_REQUEST_ID.set(requestId);
        MDC.put(MDC_KEY, requestId);
    }

    public record Snapshot(String requestId) {

        public Scope restore() {
            return RequestIdContext.open(requestId);
        }
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {

        @Override
        void close();
    }

}