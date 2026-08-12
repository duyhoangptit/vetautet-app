package com.vetautet.app.presentation.rest.filter;

import com.vetautet.app.shared.common.context.RequestIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String MDC_CLIENT_IP = "clientIp";
    private static final String MDC_METHOD = "method";
    private static final String MDC_STATUS = "status";
    private static final String MDC_DURATION_MS = "durationMs";
    private static final String MDC_URI = "uri";
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String ACTUATOR_PATH_PREFIX = "/actuator/";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = RequestIdContext.resolveRequestId(request.getHeader(RequestIdContext.HTTP_HEADER_NAME));
        request.setAttribute(RequestIdContext.REQUEST_ATTRIBUTE_NAME, requestId);
        response.setHeader(RequestIdContext.HTTP_HEADER_NAME, requestId);
        long startedAt = System.nanoTime();
        Throwable failure = null;

        RequestIdContext.Scope scope = RequestIdContext.open(requestId);
        try {
            scope.hashCode();
            filterChain.doFilter(request, response);
        } catch (Throwable ex) {
            failure = ex;
            throw ex;
        } finally {
            // đảm bảo mọi request đi vào app đều có log
            logRequestCompleted(request, response, requestId, startedAt, failure);
            scope.close();
        }
    }

    private void logRequestCompleted(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String requestId,
                                     long startedAt,
                                     Throwable failure) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        int status = response.getStatus();
        if (failure != null && status < HttpServletResponse.SC_BAD_REQUEST) {
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        // Loại bỏ log không cần thiết
        if (isNoiseRequest(request, status)) {
            return;
        }

        MDC.put(MDC_CLIENT_IP, resolveClientIp(request));
        MDC.put(MDC_METHOD, request.getMethod());
        MDC.put(MDC_STATUS, String.valueOf(status));
        MDC.put(MDC_DURATION_MS, String.valueOf(durationMs));
        MDC.put(MDC_URI, request.getRequestURI());

        try {
            log.info("HTTP request completed method={} path={} status={} durationMs={} requestId={}",
                    request.getMethod(), request.getRequestURI(), status, durationMs, requestId);
        } finally {
            MDC.remove(MDC_CLIENT_IP);
            MDC.remove(MDC_METHOD);
            MDC.remove(MDC_STATUS);
            MDC.remove(MDC_DURATION_MS);
            MDC.remove(MDC_URI);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isNoiseRequest(HttpServletRequest request, int status) {
        return status < HttpServletResponse.SC_BAD_REQUEST
                && request.getRequestURI().startsWith(ACTUATOR_PATH_PREFIX);
    }
}
