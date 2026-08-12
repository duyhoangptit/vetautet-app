# Rate Limit Module Design Prompt

Use this prompt to regenerate the current rate-limit module under package `com.vetautet.app.presentation.config.ratelimit` with behavior equivalent to the existing source.

## Goal

Implement a generic MVC-layer, per-IP anti-abuse module for public endpoints.

The module must:

- protect controller methods annotated with `@RateLimit(scope = "...")`
- read per-scope thresholds from `app.rate-limit.scopes.*` in `application.yml`
- enforce a two-tier limit backed by Redis via Redisson
- require captcha after exceeding a soft per-minute threshold
- temporarily block the IP after exceeding a hard per-hour threshold
- fail closed when captcha verification is not configured
- return a consistent HTTP 429 JSON body using the app's `BaseResponse<T>` format

## Files To Generate

Generate exactly these files in this package:

- `CaptchaVerifier.java`
- `IpRateLimiter.java`
- `NoOpCaptchaVerifier.java`
- `RateLimit.java`
- `RateLimitDecision.java`
- `RateLimitInterceptor.java`
- `RateLimitProperties.java`
- `RateLimitResponseWriter.java`

Also assume there is an existing `WebConfig` class in `com.vetautet.app.presentation.config` that registers `RateLimitInterceptor` globally via `InterceptorRegistry#addInterceptor(...)`.

## External Dependencies And Existing Types

Use these existing framework and app dependencies:

- Spring MVC interceptor API
- Spring Boot `@ConfigurationProperties`
- Redisson `RedissonClient`, `RBucket`, `RAtomicLong`
- Lombok annotations where appropriate
- Jackson `ObjectMapper`
- Jakarta Servlet request/response APIs
- existing `BaseResponse<T>` in `com.vetautet.app.presentation.rest.dto.response.BaseResponse`

Important behavior of `BaseResponse<T>` used by this module:

- it has fields `timestamp`, `status`, `error`, `errorCode`, `message`, `path`, `payload`
- for the 429 response, build it manually with the builder instead of using `BaseResponse.error(...)`

## High-Level Design

This module is the single reusable anti-abuse layer for public endpoints such as:

- `@PostMapping("/register")` with `@RateLimit(scope = "register")`
- `@GetMapping("/availability")` with `@RateLimit(scope = "availability")`

Design intent:

- registration and availability are protected without adding endpoint-specific limiter classes
- interceptor runs before request body parsing, validation, and controller execution
- scope-specific settings prevent collisions between counters for different endpoints
- captcha is abstracted behind a port so the provider can be swapped later without changing limiter/interceptor code

## Required Behavior By File

### 1. `CaptchaVerifier.java`

Create a simple interface:

- package `com.vetautet.app.presentation.config.ratelimit`
- one method: `boolean verify(String token, String clientIp);`

Document it as a port for captcha providers such as reCAPTCHA v3 or Turnstile.

Behavioral note:

- until a real provider exists, the implementation must fail closed so a challenged IP cannot bypass the challenge by sending any arbitrary header

### 2. `NoOpCaptchaVerifier.java`

Implement `CaptchaVerifier` as the default Spring bean.

Requirements:

- annotate with `@Component`
- annotate with Lombok `@Slf4j`
- method `verify(...)` must always return `false`
- log a warning containing the client IP when captcha verification is requested without a configured provider

This is intentional fail-closed behavior.

### 3. `RateLimit.java`

Create a method-level annotation.

Requirements:

- target: `ElementType.METHOD`
- retention: runtime
- attribute: `String scope() default "";`

Intent:

- marks controller methods protected by the rate-limit module
- the scope matches `app.rate-limit.scopes.<scope>` in configuration
- enforcement is done by a single global interceptor, not by per-endpoint wiring

### 4. `RateLimitDecision.java`

Create a Java record:

- `public record RateLimitDecision(boolean allowed, boolean requiresCaptcha, boolean blocked)`

Provide exactly three factory methods:

- `allow()` -> `(true, false, false)`
- `challenge()` -> `(false, true, false)`
- `block()` -> `(false, false, true)`

### 5. `RateLimitProperties.java`

Create a Spring Boot configuration-properties component.

Requirements:

- annotate class with `@Component`
- annotate class with `@ConfigurationProperties(prefix = "app.rate-limit")`
- annotate with Lombok `@Data`
- field: `private Map<String, RateLimitSpec> scopes = new HashMap<>();`
- nested static class `RateLimitSpec` with Lombok `@Data`

`RateLimitSpec` fields:

- `int softLimitPerMinute`
- `int hardLimitPerHour`
- `long challengeTtlMinutes`
- `long blockTtlMinutes`

Binding must rely on Spring Boot relaxed binding so YAML kebab-case fields map to these camel-case fields.

### 6. `IpRateLimiter.java`

This is the core Redis-backed service.

Requirements:

- annotate with `@Component`
- annotate with Lombok `@RequiredArgsConstructor`
- inject `RedissonClient` and `CaptchaVerifier`
- expose constant `public static final String CAPTCHA_TOKEN_HEADER = "X-Captcha-Token";`

#### `resolveClientIp(HttpServletRequest request)`

Behavior:

- first read `X-Forwarded-For`
- if nonblank, use the first comma-separated IP after trimming
- otherwise fall back to `request.getRemoteAddr()`

Use Spring `StringUtils.hasText(...)`.

#### `check(...)`

Signature:

`public RateLimitDecision check(String scope, String ip, String captchaToken, int softLimitPerMinute, int hardLimitPerHour, long challengeTtlMinutes, long blockTtlMinutes)`

Implement this exact decision order:

1. Build keys:
    - blocked flag: `blocked:<scope>:<ip>`
    - challenge flag: `challenge:<scope>:<ip>`
2. If blocked flag exists, immediately return `RateLimitDecision.block()`.
3. If challenge flag exists:
    - if captcha token is nonblank and `captchaVerifier.verify(token, ip)` returns `true`:
        - clear the challenge flag
        - reset the current minute counter for this scope and IP
        - return `RateLimitDecision.allow()`
    - otherwise return `RateLimitDecision.challenge()`
4. Increment minute counter using key format:
    - `ratelimit:<scope>:minute:<ip>:<minuteBucket>`
    - TTL for the minute counter must be 90 seconds
5. If minute counter value is greater than `softLimitPerMinute`:
    - set challenge flag with TTL `challengeTtlMinutes`
    - return `RateLimitDecision.challenge()`
6. Increment hour counter using key format:
    - `ratelimit:<scope>:hour:<ip>:<hourBucket>`
    - TTL for the hour counter must be 3700 seconds
7. If hour counter value is greater than `hardLimitPerHour`:
    - set blocked flag with TTL `blockTtlMinutes`
    - return `RateLimitDecision.block()`
8. Otherwise return `RateLimitDecision.allow()`.

#### Helper behavior

Implement private helpers with these responsibilities:

- `isFlagged(String key)` -> `RBucket<String>.isExists()`
- `flag(String key, long ttlMinutes)` -> set value `"1"` with `Duration.ofMinutes(ttlMinutes)`
- `clearFlag(String key)` -> delete the bucket
- `resetMinuteCounter(String scope, String ip)` -> delete current minute bucket key only
- `incrementWithTtl(String key, long ttlSeconds)`:
    - increment an `RAtomicLong`
    - if value becomes `1`, set expiry using `Duration.ofSeconds(ttlSeconds)`
    - return the incremented value
- `minuteBucket()` -> `Instant.now().getEpochSecond() / 60`
- `hourBucket()` -> `Instant.now().getEpochSecond() / 3600`

Important semantic details:

- challenge is checked before incrementing counters
- block is checked before challenge
- a successful captcha clears the challenge flag and resets only the current minute counter
- a successful captcha does not reset the hour counter
- while `NoOpCaptchaVerifier` is active, challenged IPs remain challenged until TTL expiry

### 7. `RateLimitInterceptor.java`

Create the single MVC enforcement point.

Requirements:

- implement `HandlerInterceptor`
- annotate with `@Component`
- annotate with Lombok `@RequiredArgsConstructor` and `@Slf4j`
- inject `IpRateLimiter`, `RateLimitResponseWriter`, `RateLimitProperties`

Implement `preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)` with this flow:

1. If `handler` is not a `HandlerMethod`, return `true`.
2. Read `@RateLimit` from the method.
3. If annotation is absent, return `true`.
4. Look up scope config from `properties.getScopes()`.
5. The lookup expression should match current source behavior:
    - use `rateLimit.scope() == null ? handlerMethod.getMethod().getName() : rateLimit.scope()` as the map key expression
6. If no config is found:
    - log a warning that the annotation scope has no matching `app.rate-limit.scopes` entry
    - allow the request through by returning `true`
7. Resolve client IP via `rateLimiter.resolveClientIp(request)`.
8. Read captcha token from header `IpRateLimiter.CAPTCHA_TOKEN_HEADER`.
9. Call `rateLimiter.check(...)` with:
    - annotation scope
    - resolved IP
    - captcha token
    - soft limit, hard limit, challenge TTL, block TTL from the matched spec
10. If decision is not allowed:
- log a warning including request URI, IP, scope, `requiresCaptcha`, and `blocked`
- write the 429 response via `responseWriter.writeTooManyRequests(...)`
- return `false`
1. Otherwise return `true`.

Important intent:

- interceptor runs before controller argument resolution and validation
- only annotated methods are affected even though the interceptor is globally registered

### 8. `RateLimitResponseWriter.java`

Create a reusable HTTP 429 writer.

Requirements:

- annotate with `@Component`
- annotate with Lombok `@RequiredArgsConstructor`
- inject `ObjectMapper`

Method signature:

`public void writeTooManyRequests(HttpServletResponse response, HttpServletRequest request, RateLimitDecision decision) throws IOException`

Exact behavior:

- if `decision.blocked()` is true, message is:
    - `IP temporarily blocked due to repeated requests`
- otherwise message is:
    - `Too many requests, captcha verification required`
- payload is a `Map<String, Object>` with exactly:
    - `requiresCaptcha` -> `decision.requiresCaptcha()`
    - `blocked` -> `decision.blocked()`
- build body as `BaseResponse<Map<String, Object>>` with:
    - `timestamp = Instant.now()`
    - `status = 429`
    - `error = "Too Many Requests"`
    - `message = message above`
    - `path = request.getRequestURI()`
    - `payload = payload map`
- set HTTP status to 429
- set content type to `application/json`
- serialize with `objectMapper.writeValueAsString(body)` and write to response

Do not include an `errorCode` in this 429 response.

## Required Redis Key Scheme

Use these exact key formats:

- blocked flag: `blocked:<scope>:<ip>`
- challenge flag: `challenge:<scope>:<ip>`
- minute counter: `ratelimit:<scope>:minute:<ip>:<minuteBucket>`
- hour counter: `ratelimit:<scope>:hour:<ip>:<hourBucket>`

## Required Request/Response Contract

### Captcha input

- client sends captcha token in HTTP header `X-Captcha-Token`
- this avoids changing GET query parameters or POST request DTOs

### Rejection response

Return HTTP 429 with JSON equivalent to this shape:

```json
{
  "timestamp": "2026-08-06T12:34:56Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Too many requests, captcha verification required",
  "path": "/api/v1/users/availability",
  "payload": {
    "requiresCaptcha": true,
    "blocked": false
  }
}
```

For blocked requests, keep the same shape but set:

- `message = "IP temporarily blocked due to repeated requests"`
- `payload.blocked = true`
- `payload.requiresCaptcha = false`

## Required Configuration Example

The generated design must assume the following YAML structure under `application.yml`:

```yaml
app:
  rate-limit:
    scopes:
      availability:
        soft-limit-per-minute: 20
        hard-limit-per-hour: 200
        challenge-ttl-minutes: 15
        block-ttl-minutes: 60
      register:
        soft-limit-per-minute: 5
        hard-limit-per-hour: 20
        challenge-ttl-minutes: 30
        block-ttl-minutes: 120
```

Explain that new endpoints only need:

- a new `@RateLimit(scope = "...")` annotation on the controller method
- a matching `app.rate-limit.scopes.<scope>` entry in YAML

No new Java limiter class should be required.

## Integration Points Outside This Package

Assume these integration points already exist or must remain compatible:

- `WebConfig` globally registers `RateLimitInterceptor`
- `AuthController#register(...)` uses `@RateLimit(scope = "register")`
- `UserAvailabilityController#checkAvailability(...)` uses `@RateLimit(scope = "availability")`

## Logging Requirements

Include warning logs for these situations:

- captcha verification attempted while no real provider is configured
- a method is annotated with `@RateLimit` but has no matching configuration entry
- a request is rejected, including URI, IP, scope, and decision flags

## Non-Goals

Do not add:

- endpoint-specific limiter classes
- request DTO changes for captcha fields
- custom exception types for rate limiting
- persistence outside Redis
- background cleanup jobs
- a real captcha provider implementation

## Style Constraints

Keep implementation aligned with current source style:

- concise class-level JavaDoc comments
- Lombok for boilerplate reduction
- Spring stereotype annotations on concrete beans
- simple imperative control flow
- no extra abstraction layers beyond the captcha port

## Acceptance Checklist

The generated source is correct only if all of the following are true:

- package and file names match exactly
- `RateLimitInterceptor` is generic and annotation-driven
- counters are partitioned by both scope and IP
- soft-limit overflow triggers captcha challenge, not immediate block
- hard-limit overflow triggers temporary block
- captcha success clears challenge state and resets only the current minute counter
- `NoOpCaptchaVerifier` always rejects and therefore preserves fail-closed behavior
- 429 responses use the app's `BaseResponse` shape with the exact messages above
- new endpoints can opt in with only annotation plus YAML config