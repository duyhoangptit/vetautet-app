# Login Account Lockout + Redis Fail-Open — Design

Date: 2026-08-06
Status: Approved, implementing

## Goal

Prevent password guessing against `POST /api/v1/auth/login` without introducing a
new self-inflicted denial-of-service risk when Redis is unavailable.

## Scope

1. **Account lockout on login** — 5 consecutive wrong-password attempts locks the
   account for 30 minutes. The lock clears automatically on a successful password
   reset (`reset-password` completing with `passwordChanged = true`), or on a
   subsequent successful login.
2. **Redis outage fail-open** — the three Redis-backed request-throttling layers
   (`RateLimitAspect` / `@RateLimited`, `IpRateLimiter` / `@RateLimit`,
   `AuthRateLimitFilter`) must not turn a Redis outage into a full outage of the
   auth API. They fail open (log + allow) instead of throwing 500s.
3. **OTP brute-force** — audited, already sufficient. `OtpService.verifyOtp` expires
   the OTP session after `app.auth.otp-max-attempts` (default 5) wrong codes,
   forcing a fresh `login`/`forgot-password` call to get a new OTP. No change.

## Why DB, not Redis, for the lockout state

The lockout is a security control that must survive a Redis restart/flush/outage —
losing it silently (e.g. via Redis eviction) would quietly disable brute-force
protection. `User.failedLoginAttempts` / `User.lockedUntil` are persisted columns on
`users`, written in the same transaction as the existing user lookup/save, so lockout
state never depends on Redis being up.

## Data model

`User` (domain) gains:
- `int failedLoginAttempts` (default 0)
- `Instant lockedUntil` (null = not locked)

New domain methods: `isLocked(Instant now)`, `recordFailedLogin(int maxAttempts, Duration lockDuration, Instant now)`,
`recordSuccessfulLogin()`, `unlockLogin()`.

`UserJpaEntity` / `UserEntityMapper` mirror the two fields. Migration
`015-add-login-lockout-to-users.sql` adds `failed_login_attempts INT NOT NULL DEFAULT 0`
and `locked_until TIMESTAMP` to `users`.

## Config

```yaml
app:
  auth:
    login-max-failed-attempts: 5
    login-lockout-minutes: 30
```

## Error handling

New `ErrorCode.ACCOUNT_LOCKED` (`ERR-4008`), thrown via the existing `AppLogicException`
(→ HTTP 400, same status class as `INVALID_CREDENTIALS`). Message carries max attempts
and remaining minutes as i18n args, added to all three `messages*.properties` files.

## Use-case changes

`LoginUseCaseImpl.execute()`: check `user.isLocked(now)` before comparing the password
(saves a bcrypt compare on an already-doomed request). On wrong password, persist
`recordFailedLogin(...)`; if that call just crossed the threshold, respond
`ACCOUNT_LOCKED` instead of `INVALID_CREDENTIALS`. On correct password, persist
`recordSuccessfulLogin()` if the counter was non-zero.

`ResetPasswordUseCaseImpl.execute()`: when `passwordChanged = true`, also clear
`failedLoginAttempts`/`lockedUntil` on the saved user (`unlockLogin()`).

No change to `@RateLimited` on `login` (still a useful blanket per-email throttle) or
to `AuthRateLimitFilter` (still the per-IP first line of defense) — they compose with
the new DB-backed lockout, not replace it.

## Redis outage fail-open

`RateLimitAspect.enforce`, `IpRateLimiter.check`, and `AuthRateLimitFilter.doFilterInternal`
each wrap their Redisson calls in `catch (RedisException ex)`, log a warning, and allow
the request through instead of propagating (which today surfaces as an unhandled 500,
or — for the filter, which runs before `DispatcherServlet` — an inconsistent raw error
page).

**Accepted residual risk:** during a Redis outage, `register`, `register/activate`,
`forgot-password`, and `reset-password` temporarily lose both their per-IP and per-key
throttling (no DB-backed equivalent exists for those flows). `login` /
`verify-otp-login` remain protected because the account lockout added in this change
lives in the DB, independent of Redis — provided `AuthRateLimitFilter` also fails open
(otherwise the request never reaches `LoginUseCaseImpl`). This trade-off was chosen
deliberately over fail-closed (which would turn any Redis blip into a full auth outage
for legitimate users) and matches the fail-open philosophy already established for
`@Cacheable` in `RedisConfig`/`CustomCacheErrorHandler`.

## Testing

- Unit tests on `User`: `recordFailedLogin` under threshold vs. at threshold,
  `isLocked` before/after `lockedUntil`, `recordSuccessfulLogin`/`unlockLogin` clearing
  state.
- Compile + manual review for the fail-open catch blocks (no local Redis available in
  this environment to integration-test the outage path).
