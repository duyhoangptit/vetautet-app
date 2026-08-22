# Identity Service Extraction — Design

Date: 2026-08-22
Status: Approved, ready for implementation planning

## Goal

Stand up a new, independent Spring Boot service — `identity-service` — that owns
authentication, authorization, user management, and RBAC as their own bounded
context, so they can be shared across every product in the platform (not just
`vetautet`) and evolved independently. This is the extraction step only: get
identity-service running standalone with its own DB and API surface. Cutting the
duplicated code out of `vetautet` and rewiring it to call identity-service is a
separate, later piece of work — explicitly out of scope here (see "Out of scope").

## Approach: lift-and-shift, not redesign

Copy/adapt the existing auth + user + RBAC code as-is (same domain model, same
API contracts, same behavior) into the new project. No architectural rework of
the auth/RBAC design itself in this pass — that's deliberately deferred to a
later optimization pass the user will drive once the service is standing alone.
This keeps the extraction low-risk and fast, and gives a stable baseline to
refactor from.

## Non-goals

- Modifying `vetautet` in any way (no code removed, no FK dropped, no rewiring).
- Migrating real/production data — schema + seed data only, on a fresh empty DB.
- Redesigning the domain model, JWT scheme, or RBAC data model.
- Event-driven sync (Kafka `user.*` events), service mesh, mTLS — can be added
  later if/when multiple services need to consume identity events.

## New project

- **Location:** `/Users/tigerpro/Documents/AI/vmware-ai/vetautet-app/identity-service`
  (sibling of `vetautet`), own git repository.
- **Root package:** `com.platform.identity` (not `com.vetautet.*` — this service
  is shared across products, so the package must not be brand-scoped).
- **Maven:** `groupId=com.platform`, `artifactId=identity-service`, parent
  `spring-boot-starter-parent:4.1.0`, `java.version=21` — same stack as
  `vetautet` (Spring Boot 4.1.x / Java 21 / Maven / Postgres / Redis / Liquibase),
  so the two codebases stay easy to compare while optimizing later.
- **Port:** `8081` (avoid colliding with `vetautet`'s `8080` if both run locally
  at once).

## Architecture

Same hexagonal layering as `vetautet` (`domain → application →
infrastructure/presentation`), so patterns already familiar in this codebase
(ports/adapters, mapper classes, `@RequiredArgsConstructor` services, MapStruct
DTO mapping) carry over directly:

```
com.platform.identity
├── domain/{auth,user,rbac}/{model,repository,service,exception}
├── application/{auth,user,rbac}/{dto,port/input,port/output,usecase,mapper}
├── infrastructure/{auth,persistence/jpa,security,cache,ratelimit}
├── presentation/
│   ├── rest/controller/v1        # public: /api/v1/auth/**, /api/v1/users/**
│   ├── rest/controller/internal  # internal: /internal/users/{id}, /internal/authorization/allowed-endpoints
│   ├── rest/controller/jwks      # public: /.well-known/jwks.json
│   └── config
└── shared/common/{util,exception,context}
```

RBAC (`Role`, `Endpoint`, `Portal`, `Permission`) folds into the `rbac` sub-package
even though today it's split across `domain/user` and `domain/auth` in `vetautet` —
this is a naming cleanup, not a behavior change: the classes move, their code
doesn't.

## Domain scope

**Copied (adapted to the new package root):**
- Auth: login, register (+ activation), OTP verify-login, forgot-password,
  reset-password, RSA key-pair-per-session issuance (`AuthTokenIssueService`,
  `JwtTokenGeneratorAdapter`, `RsaKeyPairRepository`/adapter), login lockout
  (`User.recordFailedLogin`/`isLocked`), `CustomJwtAuthenticationConverter`.
- User: CRUD, profile, `UserController`, `UserAvailabilityController` +
  Redis/Redisson bloom filters (`UserBloomFilterConfig`,
  `UserBloomFilterSyncService`).
- RBAC: `Role`, `Endpoint`, `Portal`, `Permission` domain models, JPA
  entities/adapters, `MultiPortalAuthorizationManager` (kept — it protects
  identity-service's own endpoints, the same way it protects `vetautet`'s
  today), `AuthorizationQuery`/`AuthorizationQueryImpl` cache-backed lookup.
- Cross-cutting: `PageableSanitizer` and its `MAX_PAGE_SIZE` cap on every
  paginated repository call (per this repo's pagination rule — the cap must be
  enforced at the persistence-adapter boundary in the new service too, not
  just at the controller), `GlobalExceptionHandler` pattern, rate-limit
  aspects/filters used by the auth endpoints (`@RateLimit`,
  `AuthRateLimitFilter`), i18n message properties for auth/user error codes.

**Not copied** (stays in `vetautet`, unrelated domain): booking, payment,
ticketing, notification, messaging, ordersdemo, and anything referencing them.

## API surface

**Public (`/api/v1/**`)** — same paths/contracts as `vetautet` today: `auth/login`,
`auth/register`, `auth/register/activate`, `auth/verify-otp-login`,
`auth/forgot-password`, `auth/reset-password`, `users/**`,
`users/availability`.

**New — internal, not in `vetautet` today** (needed because auth/user/RBAC no
longer live in the same process as the services that need them):

- `GET /.well-known/jwks.json` — public keys by `kid`, so a resource service
  (e.g. `vetautet`, later) can build its own `JwtDecoder` and validate tokens
  locally without calling back into identity-service on every request.
- `GET /internal/users/{id}` — user lookup for other services (e.g. rendering
  a user's name/email on an order).
- `GET /internal/authorization/allowed-endpoints?portal=&roles=` — same data
  `AuthorizationQueryImpl` computes today, exposed so another service's own
  `MultiPortalAuthorizationManager`-style filter can consume it instead of
  owning RBAC tables directly.

Internal endpoints are not meant for public/browser traffic: protect them with
a shared internal API key header for this first cut (simplest thing that
works; mTLS/service-mesh auth can replace it later without changing the
contract).

## Data & migration

Own Postgres instance + own Redis instance (`docker-compose-dev.yml` in the
new repo), no Kafka (nothing in the copied scope publishes/consumes Kafka
topics — `notification`/`outbox` stay in `vetautet`).

Liquibase changelogs carried over and renumbered sequentially from `001`,
scoped to what's copied:
`001-initial-schema` (users, member_info only) → `align-user-and-rsa-schema`
→ `create-auth-flow-and-otp-tables` → `inital-schema-for-rbac` →
`alter-table-otp-session` → `rbac-master-data` (seed) →
`add-login-lockout-to-users`.

Any FK that today points from a `vetautet`-domain table into `users`
(`booking_orders.user_id → users(id)`, `member_info` FK) is **not** recreated
here — those constraints live on the `vetautet` side of a future cutover and
are explicitly out of scope for this repo.

Fresh empty DB, schema + seed data only — no production data migration.

## Testing

Carry over the existing unit/`@SpringBootTest` coverage for the copied
auth/user/RBAC code; `mvn test` must be green before extraction is considered
done. The three new internal-facing endpoints (JWKS, internal user lookup,
internal allowed-endpoints) are new code and need new tests — everything else
is lift-and-shift, so no new test-writing is expected beyond adapting package
imports.

## Out of scope (explicit follow-ups, not this spec)

- Removing the duplicated auth/user/RBAC code from `vetautet`.
- Dropping the `users`-referencing FKs in `vetautet`'s DB.
- Rewiring `vetautet`'s `MultiPortalAuthorizationManager` /
  `AuthorizationQueryImpl` to call identity-service's internal API instead of
  its local RBAC tables.
- Wiring `vetautet`'s `JwtDecoder` to identity-service's JWKS endpoint instead
  of its own local `RsaKeyPairRepository`.
- Real data migration, event-driven sync, mTLS/service mesh.

Each of the above needs its own design pass once identity-service is running
and verified — they touch a live system (`vetautet`) and deserve the same
brainstorming rigor as this extraction did.
