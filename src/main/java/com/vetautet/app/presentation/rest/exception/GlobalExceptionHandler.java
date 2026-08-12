package com.vetautet.app.presentation.rest.exception;

import com.vetautet.app.domain.user.exception.DuplicateEmailException;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import com.vetautet.app.shared.common.exception.*;
import com.vetautet.app.shared.common.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
* Global exception handler for REST API
* Presentation layer - handles exceptions and converts to HTTP responses
*/
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageResolver messageResolver;

    @ExceptionHandler(AppLogicException.class)
    public ResponseEntity<BaseResponse<Void>> handleAppLogicException(
            AppLogicException ex, WebRequest request) {
        String localizedMessage = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        log.error("Application logic exception: {} - Code: {}", localizedMessage, ex.getErrorCode().getCode());

        BaseResponse<Void> error = BaseResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getErrorCode().getCode(),
                localizedMessage,
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        String localizedMessage = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        log.error("Resource not found: {} - Code: {}", localizedMessage, ex.getErrorCode().getCode());

        BaseResponse<Void> error = BaseResponse.error(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getErrorCode().getCode(),
                localizedMessage,
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<BaseResponse<Void>> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {
        String localizedMessage = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        log.error("Duplicate resource: {} - Code: {}", localizedMessage, ex.getErrorCode().getCode());

        BaseResponse<Void> error = BaseResponse.error(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getErrorCode().getCode(),
                localizedMessage,
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<BaseResponse<Void>> handleDuplicateEmailException(
            DuplicateEmailException ex, WebRequest request) {
        String localizedMessage = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        log.error("Duplicate email: {} - Code: {}", localizedMessage, ex.getErrorCode().getCode());

        BaseResponse<Void> error = BaseResponse.error(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getErrorCode().getCode(),
                localizedMessage,
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<BaseResponse<Void>> handleIdempotencyConflictException(
            IdempotencyConflictException ex, WebRequest request) {
        String localizedMessage = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        log.error("Idempotency conflict: {} - Code: {}", localizedMessage, ex.getErrorCode().getCode());

        BaseResponse<Void> error = BaseResponse.error(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getErrorCode().getCode(),
                localizedMessage,
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IdempotencyInProgressException.class)
    public ResponseEntity<BaseResponse<Void>> handleIdempotencyInProgressException(
            IdempotencyInProgressException ex, WebRequest request) {
        String localizedMessage = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        log.error("Idempotency request in progress: {} - Code: {}", localizedMessage,
                ex.getErrorCode().getCode());

        BaseResponse<Void> error = BaseResponse.error(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getErrorCode().getCode(),
                localizedMessage,
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<BaseResponse<Void>> handleRateLimitExceededException(
            RateLimitExceededException ex, WebRequest request) {
        String localizedMessage = messageResolver.getMessage(ex.getErrorCode(), ex.getMessageArgs());
        log.warn("Rate limit exceeded: {} - Code: {}", localizedMessage, ex.getErrorCode().getCode());

        BaseResponse<Void> error = BaseResponse.error(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                ex.getErrorCode().getCode(),
                localizedMessage,
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "60")
                .body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.error("Invalid argument: {}", ex.getMessage());

        BaseResponse<Void> error = BaseResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null,
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Validation failed: {}", ex.getMessage());

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        BaseResponse<Map<String, String>> error = BaseResponse.<Map<String, String>>builder()
                .timestamp(java.time.Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getDescription(false).replace("uri=", ""))
                .payload(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());

        BaseResponse<Void> error = BaseResponse.error(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                null,
                "Authentication is required to access this resource",
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        HttpStatus status = isAnonymousRequest() ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
        String message = status == HttpStatus.UNAUTHORIZED
                ? "Authentication is required to access this resource"
                : "You do not have permission to access this resource";

        log.warn("Access denied: {} - status={}", ex.getMessage(), status.value());

        BaseResponse<Void> error = BaseResponse.error(
                status.value(),
                status.getReasonPhrase(),
                null,
                message,
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException ex, WebRequest request) {
        log.debug("Static resource not found: {}", ex.getResourcePath());

        BaseResponse<Void> error = BaseResponse.error(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                null,
                "Static resource not found",
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);

        BaseResponse<Void> error = BaseResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                null,
                "An unexpected error occurred",
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private boolean isAnonymousRequest() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
    }
}
 
