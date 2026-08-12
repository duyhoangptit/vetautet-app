package com.vetautet.app.infrastructure.idempotency;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.app.domain.user.exception.DuplicateEmailException;
import com.vetautet.app.infrastructure.cache.RedisService;
import com.vetautet.app.infrastructure.persistence.jpa.entity.IdempotencyRecordJpaEntity;
import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.DuplicateResourceException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.exception.IdempotencyConflictException;
import com.vetautet.app.shared.common.exception.IdempotencyInProgressException;
import com.vetautet.app.shared.common.exception.ResourceNotFoundException;
import com.vetautet.app.shared.common.util.HashUtil;
import com.vetautet.app.shared.common.util.JsonUtil;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
* Hybrid idempotency implementation backed by DB state and Redis lock
* coordination.
*/
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {

    private static final String X_IDEMPOTENT_REPLAY = "X-Idempotent-Replay";

    private final IdempotencyRecordService recordService;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Value("${idempotency.lock.wait-time-ms:100}")
    private long lockWaitTimeMs;

    @Value("${idempotency.lock.lease-time-ms:5000}")
    private long lockLeaseTimeMs;

    @Around("@annotation(idempotent)")
    public Object enforceIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        // get config
        String operation = resolveOperation(method, idempotent);
        String idempotencyKey = resolveKey(method, joinPoint.getArgs(), idempotent.key());
        Instant expiresAt = Instant.now().plus(idempotent.ttlHours(), ChronoUnit.HOURS);
        // build request hash
        String requestHash = buildRequestHash(method, joinPoint.getArgs(), idempotent.hashFields());

        String lockKey = "idempotency:lock:" + operation + ":" + idempotencyKey;
        PreparationResult preparationResult = prepareRecord(lockKey, operation, idempotencyKey, requestHash, expiresAt,
                method);

        if (preparationResult.replayedResponse() != null) {
            return preparationResult.replayedResponse();
        }

        UUID recordId = preparationResult.recordId();
        try {
            Object result = joinPoint.proceed();
            persistOutcome(recordId, result);
            return result;
        } catch (Throwable ex) {
            recordService.markFailed(recordId, resolveErrorCode(ex), ex.getMessage());
            throw ex;
        }
    }

    private PreparationResult prepareRecord(String lockKey, String operation, String idempotencyKey,
                                            String requestHash, Instant expiresAt, Method method) throws JsonProcessingException {
        boolean locked = redisService.tryLock(lockKey, lockWaitTimeMs, lockLeaseTimeMs, TimeUnit.MILLISECONDS);
        try {
            if (locked) {
                return prepareUnderLock(operation, idempotencyKey, requestHash, expiresAt, method);
            }

            return handleExistingRecord(operation, idempotencyKey, requestHash, method, expiresAt);
        } finally {
            if (locked) {
                redisService.unlock(lockKey);
            }
        }
    }

    private PreparationResult prepareUnderLock(String operation, String idempotencyKey, String requestHash,
                                               Instant expiresAt, Method method) throws JsonProcessingException {
        Optional<IdempotencyRecordJpaEntity> existing = recordService.findByOperationAndKey(operation, idempotencyKey);
        if (existing.isPresent()) {
            return handleRecord(existing.get(), requestHash, method, expiresAt);
        }

        IdempotencyRecordJpaEntity created = recordService.createProcessing(operation, idempotencyKey, requestHash,
                expiresAt);
        return new PreparationResult(created.getIdempotencyRecordId(), null);
    }

    private PreparationResult handleExistingRecord(String operation, String idempotencyKey, String requestHash,
                                                   Method method, Instant expiresAt) throws JsonProcessingException {
        Optional<IdempotencyRecordJpaEntity> existing = recordService.findByOperationAndKey(operation, idempotencyKey);
        if (existing.isEmpty()) {
            IdempotencyRecordJpaEntity created = recordService.createProcessing(operation, idempotencyKey, requestHash,
                    expiresAt);
            return new PreparationResult(created.getIdempotencyRecordId(), null);
        }

        return handleRecord(existing.get(), requestHash, method, expiresAt);
    }

    private PreparationResult handleRecord(IdempotencyRecordJpaEntity record, String requestHash, Method method,
                                           Instant expiresAt) throws JsonProcessingException {
        if (isExpired(record)) {
            IdempotencyRecordJpaEntity reopened = recordService.reopenProcessing(record.getIdempotencyRecordId(),
                    requestHash, expiresAt);
            return new PreparationResult(reopened.getIdempotencyRecordId(), null);
        }

        ensureSamePayload(record, requestHash);

        return switch (record.getStatus()) {
            case SUCCESS -> new PreparationResult(record.getIdempotencyRecordId(), rebuildResponse(record, method));
            case PROCESSING -> throw new IdempotencyInProgressException(
                    ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS, record.getIdempotencyKey());
            case FAILED -> {
                IdempotencyRecordJpaEntity reopened = recordService.reopenProcessing(record.getIdempotencyRecordId(),
                        requestHash, expiresAt);
                yield new PreparationResult(reopened.getIdempotencyRecordId(), null);
            }
        };
    }

    private void persistOutcome(UUID recordId, Object result) {
        StoredResponse storedResponse = extractResponse(result);
        if (storedResponse.httpStatus() >= 200 && storedResponse.httpStatus() < 300) {
            recordService.markSuccess(recordId, storedResponse.httpStatus(), storedResponse.responseBody(),
                    Instant.now());
            return;
        }

        recordService.markFailed(recordId, null,
                "Request completed with non-success status: " + storedResponse.httpStatus());
    }

    private StoredResponse extractResponse(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return new StoredResponse(responseEntity.getStatusCode().value(),
                    JsonUtil.writeJson(objectMapper, responseEntity.getBody(), "Unable to serialize response"));
        }

        return new StoredResponse(200, JsonUtil.writeJson(objectMapper, result, "Unable to serialize response"));
    }

    private Object rebuildResponse(IdempotencyRecordJpaEntity record, Method method) throws JsonProcessingException {
        Object body = record.getResponseBody() == null
                ? null
                : objectMapper.readValue(record.getResponseBody(), Object.class);

        if (ResponseEntity.class.isAssignableFrom(method.getReturnType())) {
            return ResponseEntity.status(record.getHttpStatus())
                    .header(X_IDEMPOTENT_REPLAY, "true")
                    .body(body);
        }

        return body;
    }

    private String resolveOperation(Method method, Idempotent idempotent) {
        if (StringUtils.hasText(idempotent.operation())) {
            return idempotent.operation();
        }

        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    private String resolveKey(Method method, Object[] args, String expression) {
        StandardEvaluationContext context = buildEvaluationContext(method, args);
        Object value = expressionParser.parseExpression(expression).getValue(context);
        if (value == null || !StringUtils.hasText(value.toString())) {
            throw new IllegalArgumentException("Resolved idempotency key is blank");
        }

        return value.toString();
    }

    private StandardEvaluationContext buildEvaluationContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        for (int index = 0; index < args.length; index++) {
            context.setVariable("p" + index, args[index]);
            context.setVariable("a" + index, args[index]);
            if (parameterNames != null && index < parameterNames.length) {
                context.setVariable(parameterNames[index], args[index]);
            }
        }
        return context;
    }

    private String buildRequestHash(Method method, Object[] args, String[] hashFields) {
        List<Object> payload = new ArrayList<>();
        if (hashFields.length == 0) {
            Arrays.stream(args)
                    .filter(this::isHashableArgument)
                    .forEach(payload::add);
        } else {
            StandardEvaluationContext context = buildEvaluationContext(method, args);
            for (String expression : hashFields) {
                payload.add(expressionParser.parseExpression(expression).getValue(context));
            }
        }

        String jsonPayload = JsonUtil.writeJson(objectMapper, payload, "Unable to build idempotency request hash");
        return HashUtil.sha256Hex(jsonPayload);
    }

    private boolean isHashableArgument(Object argument) {
        return argument != null
                && !(argument instanceof ServletRequest)
                && !(argument instanceof ServletResponse)
                && !(argument instanceof BindingResult)
                && !(argument instanceof MultipartFile);
    }

    private void ensureSamePayload(IdempotencyRecordJpaEntity record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(ErrorCode.IDEMPOTENCY_KEY_REUSED, record.getIdempotencyKey());
        }
    }

    private boolean isExpired(IdempotencyRecordJpaEntity record) {
        return record.getExpiresAt() != null && record.getExpiresAt().isBefore(Instant.now());
    }

    private String resolveErrorCode(Throwable throwable) {
        if (throwable instanceof AppLogicException ex) {
            return ex.getErrorCode().getCode();
        }
        if (throwable instanceof DuplicateResourceException ex) {
            return ex.getErrorCode().getCode();
        }
        if (throwable instanceof DuplicateEmailException ex) {
            return ex.getErrorCode().getCode();
        }
        if (throwable instanceof ResourceNotFoundException ex) {
            return ex.getErrorCode().getCode();
        }

        return ErrorCode.INTERNAL_SERVER_ERROR.getCode();
    }

    private record PreparationResult(UUID recordId, Object replayedResponse) {
    }

    private record StoredResponse(int httpStatus, String responseBody) {
    }
}