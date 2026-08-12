package com.vetautet.app.presentation.config.ratelimit;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Writes the consistent HTTP 429 JSON body for rate-limited requests, using
 * the app's {@link BaseResponse} shape.
 */
@Component
@RequiredArgsConstructor
public class RateLimitResponseWriter {

    private final ObjectMapper objectMapper;

    public void writeTooManyRequests(HttpServletResponse response, HttpServletRequest request,
                                      RateLimitDecision decision) throws IOException {
        String message = decision.blocked()
                ? "IP temporarily blocked due to repeated requests"
                : "Too many requests, captcha verification required";

        Map<String, Object> payload = Map.of(
                "requiresCaptcha", decision.requiresCaptcha(),
                "blocked", decision.blocked());

        BaseResponse<Map<String, Object>> body = BaseResponse.<Map<String, Object>>builder()
                .timestamp(Instant.now())
                .status(429)
                .error("Too Many Requests")
                .message(message)
                .path(request.getRequestURI())
                .payload(payload)
                .build();

        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
