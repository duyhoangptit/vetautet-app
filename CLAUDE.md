# CLAUDE.md

Project-specific instructions for any AI coding agent (Claude Code and others)
working in this repository. Java 21 / Spring Boot, hexagonal architecture
(`domain` → `application` → `infrastructure` / `presentation`).

These rules override default/"common tutorial" behavior. When generating or
reviewing code, check this file first.

## Pagination — mandatory cap on `size`

**Context:** client-supplied page `size` must never reach the database
unbounded. An endpoint that does `PageRequest.of(page, size)` with a raw
`size` from the request lets any caller request an arbitrarily large page
(`size=999999999`), forcing the query to load the whole table in one shot —
unbounded resource consumption (OWASP API4:2023), risk of OOM / DB overload.
This bit the `GET /api/v1/users` and `/api/v1/users/search` endpoints; fixed
in `UserController`, `UserRepositoryAdapter`, `BookingOrderRepositoryAdapter`,
`WebConfig` — treat those as the reference implementation.

**Rules for any new or modified paginated endpoint/repository method:**

1. **The authoritative cap lives at the persistence-adapter boundary**, not
   the controller. Every call site that hands a `Pageable` to a Spring Data
   repository (`jpaRepository.findX(..., pageable)`) MUST wrap it with
   `PageableSanitizer.capped(pageable)`
   (`com.vetautet.app.shared.common.util.PageableSanitizer`). This is the
   layer every caller passes through — controller, batch job, future
   internal service — so it's the one guaranteed to run.
2. Do not invent a second hardcoded max page size anywhere. Reuse
   `PageableSanitizer.MAX_PAGE_SIZE` (currently 100). If a specific endpoint
   genuinely needs a different ceiling, add an overload/parameter, don't
   hardcode a new literal.
3. If the controller method declares `Pageable pageable` directly (the
   idiomatic Spring Data binding, not manually-parsed `int page, int size`),
   it is additionally covered by the global
   `PageableHandlerMethodArgumentResolverCustomizer` bean in
   `WebConfig` — but **do not rely on that bean alone**. It only fires when
   Spring resolves a `Pageable`-typed argument; it does nothing for
   controllers that parse `page`/`size` as separate primitives and build
   `PageRequest` by hand (which is what most controllers in this codebase
   currently do). Rule 1 is what's actually enforced end-to-end.
4. A controller-level clamp (`Math.min(size, MAX_PAGE_SIZE)` before building
   `PageRequest`) is fine as an *additional*, cheap, fail-fast layer — but
   is never a substitute for rule 1.
5. `Pageable`/`Page`/`PageRequest` classes: `org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer` —
   note the `.config` package. This project pins Spring Boot 4.1.x / Spring
   Data 4.x, where this class moved out of `org.springframework.data.web`
   (older tutorials/blogs import the pre-4.x path and will not compile here).

**Anti-pattern — do not generate code like this:**

```java
@GetMapping
public ... listX(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        ...) {
    Pageable pageable = PageRequest.of(page, size); // size is unbounded client input
    Page<X> result = someJpaRepository.findAll(pageable); // reaches the DB uncapped
    ...
}
```

**Reference implementation to follow:**
- `src/main/java/com/vetautet/app/shared/common/util/PageableSanitizer.java`
- `src/main/java/com/vetautet/app/infrastructure/persistence/jpa/adapter/UserRepositoryAdapter.java` (`findAll`, `searchByKeyword`)
- `src/main/java/com/vetautet/app/infrastructure/persistence/jpa/adapter/BookingOrderRepositoryAdapter.java` (`findByUserId`, `findByUserIdAndStatus`)
- `src/main/java/com/vetautet/app/presentation/rest/controller/v1/UserController.java` (controller-level clamp)
- `src/main/java/com/vetautet/app/presentation/config/WebConfig.java` (`pageableCustomizer` bean)

## Known open follow-ups (not yet enforced — do not assume they're fixed)

- `sortBy`/`sortDir` on `UserController.listUsers` are raw client strings fed
  into `Sort.by(direction, sortBy)` with no field whitelist. Not SQL
  injection (Spring Data derives from entity properties), but an unknown
  field throws `PropertyReferenceException` → generic 500. Whitelist against
  a fixed set of allowed sort fields before adding sortable fields to any
  paginated endpoint.
- `UserController.listUsers` / `searchUsers` are only guarded by
  `@RequireBearerAuth` (`isAuthenticated()`), i.e. **any authenticated user,
  not just admins, can list/search the entire user table.** Do not treat
  `@RequireBearerAuth` as sufficient authorization for admin-only listing
  endpoints — pair it with a role check (e.g. `@PreAuthorize("hasRole('ADMIN')")`)
  before adding endpoints that enumerate other users' data.

<!-- Add further project-wide standards below as they're established. -->
