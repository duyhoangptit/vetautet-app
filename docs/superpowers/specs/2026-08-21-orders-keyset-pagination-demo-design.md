# Orders Keyset Pagination Demo (UUIDv7, prev/next only) — Design

Date: 2026-08-21
Status: Approved, implementing

## Goal

Give a self-contained, working demonstration of the keyset (cursor) pagination
pattern described in `docs/keyset-pagination-spec.md`, against a simulated
`orders` table seeded with ~2,000,000 rows, so the OFFSET-pagination pitfalls
in that doc can be reproduced/compared against a real dataset. Navigation is
strictly sequential — **next** and **previous** only, no jump-to-page — backed
by a UUIDv7 primary key.

This is a demo/experiment feature, not a production business capability. It
must not be confused with, or modify, the existing `booking_orders` domain.

## Scope

1. New `orders` table (Postgres), 15 columns, PK `id UUID` generated as
   UUIDv7.
2. Bidirectional keyset pagination REST API: `GET /api/v1/orders-demo` with
   `after` / `before` cursor params, `size` capped at 100.
3. A dev-only seeding endpoint that bulk-inserts ~2,000,000 synthetic rows via
   JDBC batch insert.
4. Full hexagonal slice: domain → application → infrastructure →
   presentation, isolated under an `ordersdemo` package so it can't be
   mistaken for the real booking/order domain.
5. Tests covering first page, next, prev, boundary duplicates, invalid
   cursor, and end-of-data flags.

Out of scope: write/update/delete APIs for orders, filtering/sorting by
fields other than `id`, authorization/roles beyond what `@RequireBearerAuth`
already provides on the read endpoint, UI.

## Why this deviates from `docs/keyset-pagination-spec.md`

The spec's reference design assumes `id` is a `Long` with no temporal
meaning, so it composites `(created_at, id)` for both ordering and the
tie-breaker, and requires a dedicated composite index.

Here `id` is a **UUIDv7**: its leading 48 bits are a millisecond Unix
timestamp, so it is already monotonically increasing over time *and*
globally unique. That means `id` alone is sufficient as both the sort key
and the tie-breaker — no composite `(created_at, id)` condition and no extra
composite index are needed. The table's primary key index (a btree on `id`)
directly serves `WHERE id < :cursor ORDER BY id DESC LIMIT :n` and
`WHERE id > :cursor ORDER BY id ASC LIMIT :n` as index-seek range scans.
`created_at` is kept as a plain display/business column, generated in lock
step with `id`'s embedded timestamp during seeding, but it is **not** part
of the cursor condition.

The spec's API shape (`cursor` + implicit forward-only paging) is extended
to two independent cursor params, `after` and `before`, to support true
previous-page navigation (see §5).

## Package layout

New, isolated package tree — does not touch `domain.booking` or any existing
booking/order code:

```
domain/ordersdemo/model/Order.java
domain/ordersdemo/model/OrderStatus.java
domain/ordersdemo/repository/OrderRepository.java            (port)

application/ordersdemo/dto/OrderCursor.java
application/ordersdemo/dto/OrderKeysetPage.java
application/ordersdemo/usecase/GetOrdersPageUseCase.java
application/ordersdemo/usecase/SeedOrdersUseCase.java

infrastructure/persistence/jpa/entity/OrderJpaEntity.java
infrastructure/persistence/jpa/repository/OrderJpaRepository.java
infrastructure/persistence/jpa/adapter/OrderRepositoryAdapter.java
infrastructure/persistence/jpa/mapper/OrderEntityMapper.java
infrastructure/ordersdemo/seed/OrderSeedJdbcWriter.java
infrastructure/ordersdemo/seed/FakeOrderDataFactory.java

shared/common/util/Uuid7Generator.java

presentation/rest/controller/v1/OrderDemoController.java
presentation/rest/controller/internal/OrderSeedController.java
presentation/rest/dto/response/OrderResponse.java
presentation/rest/dto/response/OrderKeysetPageResponse.java
```

## Data model

Table `orders` (migration `017-create-orders-demo-table.sql`; RBAC grant for
the read endpoint in a second migration, `018-orders-demo-rbac.sql` — see
API section), 15 columns:

| Column             | Type            | Notes                                   |
|---------------------|-----------------|------------------------------------------|
| `id`                | UUID PK         | UUIDv7, generated in application code    |
| `order_code`        | VARCHAR(40)     | unique, e.g. `ORD-xxxxxxxx`              |
| `customer_name`     | VARCHAR(150)    |                                           |
| `customer_email`    | VARCHAR(150)    |                                           |
| `customer_phone`    | VARCHAR(30)     |                                           |
| `status`            | VARCHAR(20)     | enum string, see `OrderStatus`           |
| `total_amount`      | NUMERIC(18,2)   |                                           |
| `currency`          | VARCHAR(3)      | e.g. `VND`, `USD`                        |
| `quantity`          | INT             |                                           |
| `payment_method`    | VARCHAR(30)     |                                           |
| `shipping_address`  | VARCHAR(255)    |                                           |
| `shipping_city`     | VARCHAR(100)    |                                           |
| `notes`             | VARCHAR(500)    | nullable                                 |
| `created_at`        | TIMESTAMPTZ     | matches `id`'s embedded UUIDv7 timestamp |
| `updated_at`        | TIMESTAMPTZ     |                                           |

`OrderStatus`: `PENDING, CONFIRMED, PAID, CANCELLED, REFUNDED, COMPLETED`.

No secondary index beyond the PK — no filtering/sorting outside `id` is in
scope (YAGNI; add one later if a filtered demo endpoint is added).

## UUIDv7 generation — `Uuid7Generator`

Self-contained utility, no new dependency, implements RFC 9562 §5.7:

- 48 bits: Unix epoch milliseconds
- 4 bits: version (`0111`)
- 12 bits: random
- 2 bits: variant (`10`)
- 62 bits: random

API:
```java
public static UUID generate();              // uses Instant.now()
public static UUID generate(Instant ts);     // explicit timestamp, for seeding
```
Backed by `SecureRandom`. No strict same-millisecond monotonic counter — not
required here since seeding spaces timestamps ~7.8ms apart (see §6) and
within-millisecond collisions among real traffic are an accepted, documented
trade-off for a demo utility.

## Cursor — `OrderCursor`

```java
public record OrderCursor(UUID id) {
    public String encode();                       // Base64 of id.toString()
    public static OrderCursor decode(String raw);  // throws AppLogicException(INVALID_ORDER_CURSOR) on tamper/garbage
}
```

Decode failure (not valid Base64, not a valid UUID) throws
`AppLogicException(ErrorCode.INVALID_ORDER_CURSOR)` — reuses the existing
`GlobalExceptionHandler` handler for `AppLogicException`, which already maps
to HTTP 400. No new exception class or handler needed. Add
`INVALID_ORDER_CURSOR("ERR-3010")` to `ErrorCode` (next free code in the
existing validation-errors 3xxx block) plus a message key in the i18n
properties files.

## API

`GET /api/v1/orders-demo?after={cursor}&before={cursor}&size=20`

- `after` XOR `before`, never both — both present throws
  `AppLogicException(ErrorCode.INVALID_ORDER_CURSOR)` (400).
- Neither present → first page.
- `size`: default 20, hard cap 100. Capped in two layers per CLAUDE.md:
  controller-level `Math.min(size, PageableSanitizer.MAX_PAGE_SIZE)` as a
  fail-fast check, and again in `OrderRepositoryAdapter` immediately before
  the native query is built (the authoritative boundary — this path uses a
  hand-built `LIMIT`, not a Spring Data `Pageable`, so `PageableSanitizer`
  itself isn't applicable, but its `MAX_PAGE_SIZE` constant is reused per
  CLAUDE.md rule 2; no second hardcoded literal).
- Protected by `@RequireBearerAuth`. **Addendum (discovered during planning,
  not in the original design pass):** every non-`permitAll` route in this
  codebase also passes through `MultiPortalAuthorizationManager`, which
  denies (403) unless the caller's role has an explicit RBAC grant in the
  `endpoints` / `permissions` / `role_permissions` tables (seeded in
  `014-rbac-master-data.sql`) — `@RequireBearerAuth` alone (`isAuthenticated()`)
  is necessary but not sufficient, same caveat CLAUDE.md already flags for
  `UserController`. This endpoint is registered **ADMIN-only**: a new
  migration adds a `ordersdemo:orders:list` permission + the
  `GET /api/v1/orders-demo` endpoint row + a `permission_endpoints` link.
  No `role_permissions` row is needed for ADMIN specifically — that role's
  seed already grants it every permission via `JOIN permissions permission
  ON TRUE`. Deliberately not granted to the `USER` role: this lists raw
  synthetic order data across the whole table with no ownership scoping,
  so it stays an admin/dev tool, not a customer-facing listing.

Response body (`OrderKeysetPageResponse`):
```json
{
  "content": [ { "id": "...", "orderCode": "...", "...": "..." } ],
  "nextCursor": "base64-or-null",
  "prevCursor": "base64-or-null",
  "hasNext": true,
  "hasPrevious": false
}
```

### Query semantics (default sort: newest first, `id DESC`)

| Request       | Query                                                        | `hasNext`                | `hasPrevious` |
|---------------|---------------------------------------------------------------|---------------------------|---------------|
| no cursor     | `ORDER BY id DESC LIMIT size+1`                                | `rows.size() > size`      | `false`       |
| `after=c`     | `WHERE id < :c ORDER BY id DESC LIMIT size+1`                  | `rows.size() > size`      | `true`        |
| `before=c`    | `WHERE id > :c ORDER BY id ASC LIMIT size+1`, then reverse to DESC | `true`                | `rows.size() > size` |

The `size+1`-row probe (spec §3.4) avoids a separate `COUNT` query. The extra
probe row is trimmed before returning `content`. `nextCursor` encodes the
`id` of the last row in `content` (only when `hasNext`); `prevCursor` encodes
the `id` of the first row (only when `hasPrevious`).

This is a documented simplification versus a fully symmetric Relay-style
cursor: `hasPrevious` on an `after` page (and `hasNext` on a `before` page)
is inferred from "a cursor was supplied, so there is necessarily a row on
the other side" rather than a second probe query. Correct for this demo's
sequential next/prev-only navigation; would need revisiting if jump-to-page
or a "refresh from cursor" use case were added later.

### Edge cases handled

1. **Duplicate boundary values** — impossible by construction: `id` is a
   unique PK, so `id < :cursor` / `id > :cursor` never re-includes or skips
   the boundary row itself.
2. **Tampered/invalid cursor** — decode failure → 400 via
   `AppLogicException`, never a raw stack trace.
3. **First page** — no cursor → plain `ORDER BY id DESC LIMIT size+1`, no
   `WHERE`.
4. **Concurrent inserts during pagination** — keyset is inherently immune
   (each page is defined relative to a specific `id`, not a row offset).
5. **No jump-to-page** — enforced by only exposing `after`/`before`, never a
   `page` number, in the API contract.

## Seeding ~2,000,000 rows

`POST /internal/orders-demo/seed?count=2000000`

- `OrderSeedController`, annotated `@Profile({"dev", "local"})` — absent from
  the bean graph entirely outside those profiles, so it cannot be reached in
  production regardless of routing/security config.
- Delegates to `SeedOrdersUseCase` → `OrderSeedJdbcWriter`, which uses
  `JdbcTemplate.batchUpdate` (not JPA/Hibernate — avoids persistence-context
  growth over 2M entities) in chunks of 5,000 rows per batch.
- Synthetic timestamps: `created_at_i = now - 180d + i * (180d / count)` for
  row `i` in `[0, count)` — strictly increasing, ~7.8ms apart at
  count=2,000,000, so `UUIDv7` values come out monotonically increasing in
  insertion order (matches real insert-time semantics, avoids visible
  millisecond collisions). `id_i = Uuid7Generator.generate(created_at_i)`;
  `created_at` column stores the same instant.
- `FakeOrderDataFactory` produces the other columns from small in-memory
  arrays of sample names/cities/statuses + `java.util.Random` — no new Faker
  dependency.
- Logs progress every 100,000 rows inserted.
- Idempotency: not required for a dev-only demo tool; calling it twice
  simply appends another `count` rows. Truncation, if needed, is a manual
  `TRUNCATE orders` — not exposed via API (avoid a destructive endpoint).

## Testing

One `@DataJpaTest` (or `@SpringBootTest` with a test slice, matching the
project's existing convention for repository-adapter tests) covering:

1. First page returns `size` rows (of a small seeded fixture, not 2M),
   `hasPrevious = false`.
2. `after=<nextCursor>` returns the next `size` rows, no overlap/gap with
   page 1.
3. `before=<prevCursor>` from page 2 returns exactly page 1's rows in the
   same order.
4. Last page: `hasNext = false`, `nextCursor = null`.
5. Invalid cursor string → `AppLogicException(INVALID_ORDER_CURSOR)`.
6. `after` and `before` both supplied → `AppLogicException(INVALID_ORDER_CURSOR)`.

## Trade-offs (kept from the source spec, restated for this table)

- No jump-to-page. If an admin dashboard later needs it, hybrid approach:
  OFFSET pagination for small/near-realtime UI lists, keyset for this kind
  of high-volume/export/infinite-scroll access — do not retrofit jump-to-page
  onto this endpoint.
- `id`-only cursor relies on UUIDv7's embedded timestamp for ordering
  semantics; if `id` generation is ever changed to a non-time-ordered scheme
  (e.g. UUIDv4), this design breaks silently (rows would no longer sort
  chronologically) — flagged here so a future change to the ID scheme
  doesn't slip through unnoticed.
