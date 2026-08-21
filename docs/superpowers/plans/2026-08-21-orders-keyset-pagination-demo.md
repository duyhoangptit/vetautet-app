# Orders Keyset Pagination Demo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a working, isolated demo of UUIDv7-keyed, prev/next-only keyset pagination over a simulated `orders` table (15 columns, ~2,000,000 seeded rows), per `docs/keyset-pagination-spec.md`.

**Architecture:** New `ordersdemo` hexagonal slice (domain → application → infrastructure → presentation), fully isolated from the existing `booking` domain. Reads use native Postgres queries keyed on `id` (UUIDv7) only — no composite index needed since UUIDv7 is itself time-ordered and unique. Writes for seeding bypass JPA entirely (raw JDBC batch insert) for throughput.

**Tech Stack:** Java 21, Spring Boot (Data JPA + JDBC + Web + Security OAuth2 Resource Server), Postgres, Liquibase, Lombok, JUnit 5 + Mockito + AssertJ (existing project stack, no new dependencies).

**Spec:** `docs/superpowers/specs/2026-08-21-orders-keyset-pagination-demo-design.md`

## Global Constraints

- Page `size` is capped at `PageableSanitizer.MAX_PAGE_SIZE` (100) — reuse this constant everywhere; never hardcode a second max (CLAUDE.md rule 2).
- The authoritative size cap lives at the persistence-adapter boundary (`OrderRepositoryAdapter`), not just the controller (CLAUDE.md rule 1).
- Cursor is `id` (UUIDv7) only — no `created_at` composite condition, no extra index beyond the primary key.
- No jump-to-page: the API only ever exposes `after`/`before` cursors, never a `page` number.
- `GET /api/v1/orders-demo` is ADMIN-only via the existing RBAC tables (`permissions`/`endpoints`/`permission_endpoints`/`role_permissions`), in addition to `@RequireBearerAuth`.
- `POST /internal/orders-demo/seed` only exists as a Spring bean under `@Profile({"dev", "local"})`; it must be added to `SecurityConfig`'s `permitAll` matchers (safety comes from profile exclusion in production, not from auth).
- Follow existing project conventions: Lombok `@Getter/@Builder` domain models, manual (non-MapStruct) entity mappers, Mockito-only unit tests (no `@DataJpaTest`/Testcontainers — none exist in this codebase), Liquibase "formatted SQL" changesets.

---

## Task 1: `Uuid7Generator` utility

**Files:**
- Create: `src/main/java/com/vetautet/app/shared/common/util/Uuid7Generator.java`
- Test: `src/test/java/com/vetautet/app/shared/common/util/Uuid7GeneratorTest.java`

**Interfaces:**
- Produces: `Uuid7Generator.generate()` → `UUID`; `Uuid7Generator.generate(Instant timestamp)` → `UUID`; `Uuid7Generator.extractTimestamp(UUID uuid)` → `Instant`. Used by later tasks (`OrderSeedJdbcWriter`'s caller in Task 11) to mint ids with an explicit synthetic timestamp.

- [ ] **Step 1: Write the failing test**

```java
package com.vetautet.app.shared.common.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class Uuid7GeneratorTest {

    @Test
    void generate_returnsVersion7Variant2Uuid() {
        UUID uuid = Uuid7Generator.generate();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void generate_withExplicitTimestamp_roundTripsThroughExtractTimestamp() {
        Instant timestamp = Instant.parse("2025-01-15T10:30:00.123Z");

        UUID uuid = Uuid7Generator.generate(timestamp);

        assertThat(Uuid7Generator.extractTimestamp(uuid))
                .isEqualTo(timestamp.truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void generate_successiveIncreasingTimestamps_produceIncreasingUuids() {
        Instant earlier = Instant.parse("2025-01-15T10:30:00.000Z");
        Instant later = Instant.parse("2025-01-15T10:30:00.500Z");

        UUID earlierUuid = Uuid7Generator.generate(earlier);
        UUID laterUuid = Uuid7Generator.generate(later);

        assertThat(earlierUuid).isLessThan(laterUuid);
    }

    @Test
    void generate_twoCallsSameInstant_produceDifferentRandomUuids() {
        Instant sameInstant = Instant.parse("2025-01-15T10:30:00.000Z");

        UUID first = Uuid7Generator.generate(sameInstant);
        UUID second = Uuid7Generator.generate(sameInstant);

        assertThat(first).isNotEqualTo(second);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=Uuid7GeneratorTest test`
Expected: FAIL to compile — `Uuid7Generator` does not exist yet.

- [ ] **Step 3: Write the implementation**

```java
package com.vetautet.app.shared.common.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Generates RFC 9562 UUIDv7 values: a 48-bit big-endian Unix-epoch-millisecond
 * timestamp in the high bits, followed by version/variant marker bits and
 * random fill. Because the timestamp is the leading bit-field, UUIDv7 values
 * sort monotonically with wall-clock time and are still globally unique -
 * exactly the property {@code OrderRepositoryAdapter}'s keyset pagination
 * relies on to use {@code id} alone as both sort key and tie-breaker (see
 * docs/superpowers/specs/2026-08-21-orders-keyset-pagination-demo-design.md).
 * <p>
 * No same-millisecond monotonic counter is implemented - within one
 * millisecond, relative order among generated ids is randomized. Acceptable
 * here: the seeder (Task 11) spaces synthetic timestamps ~milliseconds apart
 * specifically to avoid this in bulk-seeded data.
 */
public final class Uuid7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long VERSION_7 = 0x7L;
    private static final long VARIANT_RFC4122 = 0x2L;
    private static final long TIMESTAMP_MASK_48_BITS = 0xFFFFFFFFFFFFL;
    private static final long RAND_A_MASK_12_BITS = 0x0FFFL;
    private static final long RAND_B_MASK_62_BITS = 0x3FFFFFFFFFFFFFFFL;

    private Uuid7Generator() {
    }

    /** Generates a UUIDv7 using the current wall-clock time. */
    public static UUID generate() {
        return generate(Instant.now());
    }

    /**
     * Generates a UUIDv7 embedding the given timestamp (truncated to
     * millisecond precision) instead of {@code Instant.now()} - used by the
     * demo-data seeder to spread synthetic ids across a wide time range
     * instead of clustering every seeded row around "now".
     */
    public static UUID generate(Instant timestamp) {
        long unixTimeMillis = timestamp.toEpochMilli();

        byte[] randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);

        long randA = (((randomBytes[0] & 0xFFL) << 4) | ((randomBytes[1] & 0xF0L) >>> 4))
                & RAND_A_MASK_12_BITS;

        long mostSigBits = ((unixTimeMillis & TIMESTAMP_MASK_48_BITS) << 16)
                | (VERSION_7 << 12)
                | randA;

        long randB = (((randomBytes[2] & 0xFFL) << 56)
                | ((randomBytes[3] & 0xFFL) << 48)
                | ((randomBytes[4] & 0xFFL) << 40)
                | ((randomBytes[5] & 0xFFL) << 32)
                | ((randomBytes[6] & 0xFFL) << 24)
                | ((randomBytes[7] & 0xFFL) << 16)
                | ((randomBytes[8] & 0xFFL) << 8)
                | (randomBytes[9] & 0xFFL))
                & RAND_B_MASK_62_BITS;

        long leastSigBits = (VARIANT_RFC4122 << 62) | randB;

        return new UUID(mostSigBits, leastSigBits);
    }

    /** Extracts the millisecond-precision timestamp embedded in a UUIDv7's leading 48 bits. */
    public static Instant extractTimestamp(UUID uuid) {
        long unixTimeMillis = uuid.getMostSignificantBits() >>> 16;
        return Instant.ofEpochMilli(unixTimeMillis);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=Uuid7GeneratorTest test`
Expected: PASS (4/4 tests green)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/vetautet/app/shared/common/util/Uuid7Generator.java src/test/java/com/vetautet/app/shared/common/util/Uuid7GeneratorTest.java
git commit -m "feat(ordersdemo): add RFC 9562 UUIDv7 generator utility"
```

---

## Task 2: Domain model + `OrderCursor` (+ new `ErrorCode`)

**Files:**
- Create: `src/main/java/com/vetautet/app/domain/ordersdemo/model/Order.java`
- Create: `src/main/java/com/vetautet/app/domain/ordersdemo/model/OrderStatus.java`
- Create: `src/main/java/com/vetautet/app/domain/ordersdemo/repository/OrderRepository.java`
- Create: `src/main/java/com/vetautet/app/application/ordersdemo/dto/OrderCursor.java`
- Test: `src/test/java/com/vetautet/app/application/ordersdemo/dto/OrderCursorTest.java`
- Modify: `src/main/java/com/vetautet/app/shared/common/exception/ErrorCode.java`
- Modify: `src/main/resources/i18n/messages.properties`
- Modify: `src/main/resources/i18n/messages_en.properties`
- Modify: `src/main/resources/i18n/messages_vi.properties`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `Order` (Lombok builder, fields: `id UUID, orderCode String, customerName String, customerEmail String, customerPhone String, status OrderStatus, totalAmount BigDecimal, currency String, quantity Integer, paymentMethod String, shippingAddress String, shippingCity String, notes String, createdAt Instant, updatedAt Instant`); `OrderStatus` enum (`PENDING, CONFIRMED, PAID, CANCELLED, REFUNDED, COMPLETED`); `OrderRepository` port (`List<Order> findFirstPage(int limit)`, `List<Order> findNextPage(UUID afterId, int limit)`, `List<Order> findPrevPage(UUID beforeId, int limit)`); `OrderCursor` record (`UUID id`, `.encode()` → `String`, static `.decode(String)` → `OrderCursor`, throws `AppLogicException(ErrorCode.INVALID_ORDER_CURSOR)`); `ErrorCode.INVALID_ORDER_CURSOR`. Later tasks (3, 5, 8) depend on all of these exact names/signatures.

- [ ] **Step 1: Write the failing test**

```java
package com.vetautet.app.application.ordersdemo.dto;

import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderCursorTest {

    @Test
    void encodeThenDecode_roundTripsToSameId() {
        UUID id = UUID.randomUUID();
        OrderCursor cursor = new OrderCursor(id);

        String encoded = cursor.encode();
        OrderCursor decoded = OrderCursor.decode(encoded);

        assertThat(decoded.id()).isEqualTo(id);
    }

    @Test
    void decode_nullCursor_throwsInvalidOrderCursor() {
        assertThatThrownBy(() -> OrderCursor.decode(null))
                .isInstanceOf(AppLogicException.class)
                .extracting(ex -> ((AppLogicException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_CURSOR);
    }

    @Test
    void decode_blankCursor_throwsInvalidOrderCursor() {
        assertThatThrownBy(() -> OrderCursor.decode("   "))
                .isInstanceOf(AppLogicException.class);
    }

    @Test
    void decode_notValidBase64_throwsInvalidOrderCursor() {
        assertThatThrownBy(() -> OrderCursor.decode("not-valid-base64!!!"))
                .isInstanceOf(AppLogicException.class)
                .extracting(ex -> ((AppLogicException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_CURSOR);
    }

    @Test
    void decode_validBase64ButNotAUuid_throwsInvalidOrderCursor() {
        String tampered = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("not-a-uuid".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThatThrownBy(() -> OrderCursor.decode(tampered))
                .isInstanceOf(AppLogicException.class)
                .extracting(ex -> ((AppLogicException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_CURSOR);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=OrderCursorTest test`
Expected: FAIL to compile — none of `OrderCursor`, `ErrorCode.INVALID_ORDER_CURSOR` exist yet.

- [ ] **Step 3: Add `ErrorCode.INVALID_ORDER_CURSOR`**

Edit `src/main/java/com/vetautet/app/shared/common/exception/ErrorCode.java`, inside the `// Validation errors (3xxx)` block, immediately after `AUTH_FLOW_TOKEN_EXPIRED("ERR-3009"),`:

```java
    AUTH_FLOW_TOKEN_EXPIRED("ERR-3009"),

    INVALID_ORDER_CURSOR("ERR-3010"),
```

- [ ] **Step 4: Add i18n messages**

Append to the validation-messages block (right after the `ERR-3009` line) in all three files:

`src/main/resources/i18n/messages_en.properties`:
```
ERR-3010=Invalid or corrupted pagination cursor: {0}
```

`src/main/resources/i18n/messages_vi.properties` and `src/main/resources/i18n/messages.properties` (identical, matching the existing pattern of `messages.properties` mirroring `messages_vi.properties`):
```
ERR-3010=Con trỏ phân trang không hợp lệ hoặc bị hỏng: {0}
```

- [ ] **Step 5: Create the domain model**

```java
package com.vetautet.app.domain.ordersdemo.model;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAID,
    CANCELLED,
    REFUNDED,
    COMPLETED
}
```

```java
package com.vetautet.app.domain.ordersdemo.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Simulated order record for the keyset-pagination demo (see
 * docs/keyset-pagination-spec.md). Isolated from the real
 * {@code com.vetautet.app.domain.booking} model - not a business entity.
 */
@Getter
@Builder(toBuilder = true)
public class Order {

    private final UUID id;
    private final String orderCode;
    private final String customerName;
    private final String customerEmail;
    private final String customerPhone;
    private final OrderStatus status;
    private final BigDecimal totalAmount;
    private final String currency;
    private final Integer quantity;
    private final String paymentMethod;
    private final String shippingAddress;
    private final String shippingCity;
    private final String notes;
    private final Instant createdAt;
    private final Instant updatedAt;
}
```

```java
package com.vetautet.app.domain.ordersdemo.repository;

import com.vetautet.app.domain.ordersdemo.model.Order;

import java.util.List;
import java.util.UUID;

/**
 * Output port for keyset-paginated reads over the demo {@code orders} table.
 * Every method returns up to {@code limit} rows ordered by {@code id}
 * (UUIDv7) - callers request {@code size + 1} rows to detect "has more"
 * without a separate COUNT query (see spec &sect;3.4).
 */
public interface OrderRepository {

    /** First page: no cursor, newest first. */
    List<Order> findFirstPage(int limit);

    /** Rows strictly older than {@code afterId} (id &lt; afterId), newest first. */
    List<Order> findNextPage(UUID afterId, int limit);

    /**
     * Rows strictly newer than {@code beforeId} (id &gt; beforeId), returned
     * oldest first (ascending) - the caller is responsible for reversing this
     * back to newest-first display order.
     */
    List<Order> findPrevPage(UUID beforeId, int limit);
}
```

- [ ] **Step 6: Create `OrderCursor`**

```java
package com.vetautet.app.application.ordersdemo.dto;

import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Opaque keyset-pagination cursor: the {@code id} (UUIDv7) of the boundary
 * row. Base64url-encoded so it's safe to pass as a query parameter.
 */
public record OrderCursor(UUID id) {

    public String encode() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static OrderCursor decode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AppLogicException(ErrorCode.INVALID_ORDER_CURSOR, String.valueOf(raw));
        }
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(raw);
            UUID id = UUID.fromString(new String(decodedBytes, StandardCharsets.UTF_8));
            return new OrderCursor(id);
        } catch (IllegalArgumentException ex) {
            throw new AppLogicException(ErrorCode.INVALID_ORDER_CURSOR, ex, raw);
        }
    }
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `mvn -q -Dtest=OrderCursorTest test`
Expected: PASS (5/5 tests green)

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/vetautet/app/domain/ordersdemo src/main/java/com/vetautet/app/application/ordersdemo/dto/OrderCursor.java src/test/java/com/vetautet/app/application/ordersdemo/dto/OrderCursorTest.java src/main/java/com/vetautet/app/shared/common/exception/ErrorCode.java src/main/resources/i18n/
git commit -m "feat(ordersdemo): add Order domain model, OrderRepository port, OrderCursor"
```

---

## Task 3: `GetOrdersPageUseCase` + `OrderKeysetPage`

**Files:**
- Create: `src/main/java/com/vetautet/app/application/ordersdemo/dto/OrderKeysetPage.java`
- Create: `src/main/java/com/vetautet/app/application/ordersdemo/port/input/GetOrdersPageUseCase.java`
- Create: `src/main/java/com/vetautet/app/application/ordersdemo/usecase/GetOrdersPageUseCaseImpl.java`
- Test: `src/test/java/com/vetautet/app/application/ordersdemo/usecase/GetOrdersPageUseCaseImplTest.java`

**Interfaces:**
- Consumes: `Order`, `OrderRepository` (Task 2); `OrderCursor` (Task 2); `PageableSanitizer.MAX_PAGE_SIZE` (existing, `com.vetautet.app.shared.common.util.PageableSanitizer`).
- Produces: `OrderKeysetPage` record (`List<Order> content, String nextCursor, String prevCursor, boolean hasNext, boolean hasPrevious`, Lombok `@Builder`); `GetOrdersPageUseCase.execute(String afterCursor, String beforeCursor, int size)` → `OrderKeysetPage`. Task 8 (controller) depends on this exact signature.

- [ ] **Step 1: Write the failing test**

```java
package com.vetautet.app.application.ordersdemo.usecase;

import com.vetautet.app.application.ordersdemo.dto.OrderCursor;
import com.vetautet.app.application.ordersdemo.dto.OrderKeysetPage;
import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import com.vetautet.app.domain.ordersdemo.repository.OrderRepository;
import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrdersPageUseCaseImplTest {

    @Mock
    private OrderRepository orderRepository;

    private GetOrdersPageUseCaseImpl useCase;

    private final UUID id1 = UUID.fromString("00000000-0000-7000-8000-000000000001");
    private final UUID id2 = UUID.fromString("00000000-0000-7000-8000-000000000002");
    private final UUID id3 = UUID.fromString("00000000-0000-7000-8000-000000000003");

    private Order order(UUID id) {
        return Order.builder()
                .id(id)
                .orderCode("ORD-" + id)
                .customerName("Test Customer")
                .customerEmail("test@example.com")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .currency("VND")
                .quantity(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        useCase = new GetOrdersPageUseCaseImpl(orderRepository);
    }

    @Test
    void execute_noCursor_hasMoreRows_returnsFirstPageWithNextCursorOnly() {
        // size=2 -> probe limit 3; repo returns 3 rows meaning there's a 4th+ row beyond
        when(orderRepository.findFirstPage(3)).thenReturn(List.of(order(id1), order(id2), order(id3)));

        OrderKeysetPage page = useCase.execute(null, null, 2);

        assertThat(page.content()).extracting(Order::getId).containsExactly(id1, id2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.prevCursor()).isNull();
        assertThat(OrderCursor.decode(page.nextCursor()).id()).isEqualTo(id2);
    }

    @Test
    void execute_noCursor_lastPage_hasNextFalseAndNullCursor() {
        when(orderRepository.findFirstPage(3)).thenReturn(List.of(order(id1), order(id2)));

        OrderKeysetPage page = useCase.execute(null, null, 2);

        assertThat(page.content()).hasSize(2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void execute_afterCursor_delegatesToFindNextPage_andHasPreviousIsTrue() {
        String after = new OrderCursor(id1).encode();
        when(orderRepository.findNextPage(id1, 3)).thenReturn(List.of(order(id2), order(id3)));

        OrderKeysetPage page = useCase.execute(after, null, 2);

        assertThat(page.content()).extracting(Order::getId).containsExactly(id2, id3);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isFalse();
        assertThat(OrderCursor.decode(page.prevCursor()).id()).isEqualTo(id2);
    }

    @Test
    void execute_beforeCursor_reversesAscendingRepoResultBackToDescendingDisplayOrder() {
        String before = new OrderCursor(id3).encode();
        // repo returns ascending (oldest-first): id1 then id2, for a 2-row page (no more beyond)
        when(orderRepository.findPrevPage(id3, 3)).thenReturn(List.of(order(id1), order(id2)));

        OrderKeysetPage page = useCase.execute(null, before, 2);

        assertThat(page.content()).extracting(Order::getId).containsExactly(id2, id1);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.prevCursor()).isNull();
    }

    @Test
    void execute_bothAfterAndBeforeSupplied_throwsInvalidOrderCursor() {
        String after = new OrderCursor(id1).encode();
        String before = new OrderCursor(id2).encode();

        assertThatThrownBy(() -> useCase.execute(after, before, 20))
                .isInstanceOf(AppLogicException.class)
                .extracting(ex -> ((AppLogicException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_CURSOR);
    }

    @Test
    void execute_sizeAboveMaxPageSize_isCappedBeforeQuerying() {
        when(orderRepository.findFirstPage(101)).thenReturn(List.of(order(id1)));

        useCase.execute(null, null, 999);

        org.mockito.Mockito.verify(orderRepository).findFirstPage(101);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=GetOrdersPageUseCaseImplTest test`
Expected: FAIL to compile — `OrderKeysetPage`, `GetOrdersPageUseCaseImpl` don't exist yet.

- [ ] **Step 3: Create `OrderKeysetPage`**

```java
package com.vetautet.app.application.ordersdemo.dto;

import com.vetautet.app.domain.ordersdemo.model.Order;
import lombok.Builder;

import java.util.List;

@Builder
public record OrderKeysetPage(
        List<Order> content,
        String nextCursor,
        String prevCursor,
        boolean hasNext,
        boolean hasPrevious) {
}
```

- [ ] **Step 4: Create the input port**

```java
package com.vetautet.app.application.ordersdemo.port.input;

import com.vetautet.app.application.ordersdemo.dto.OrderKeysetPage;

/**
 * Input port for the orders-demo keyset pagination endpoint. Exactly one of
 * {@code afterCursor} / {@code beforeCursor} may be non-blank; both blank
 * means "first page". No page-number parameter exists by design - next/prev
 * only (see docs/keyset-pagination-spec.md).
 */
public interface GetOrdersPageUseCase {
    OrderKeysetPage execute(String afterCursor, String beforeCursor, int size);
}
```

- [ ] **Step 5: Implement the use case**

```java
package com.vetautet.app.application.ordersdemo.usecase;

import com.vetautet.app.application.ordersdemo.dto.OrderCursor;
import com.vetautet.app.application.ordersdemo.dto.OrderKeysetPage;
import com.vetautet.app.application.ordersdemo.port.input.GetOrdersPageUseCase;
import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.repository.OrderRepository;
import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.util.PageableSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOrdersPageUseCaseImpl implements GetOrdersPageUseCase {

    private final OrderRepository orderRepository;

    @Override
    public OrderKeysetPage execute(String afterCursor, String beforeCursor, int size) {
        boolean hasAfter = StringUtils.hasText(afterCursor);
        boolean hasBefore = StringUtils.hasText(beforeCursor);
        if (hasAfter && hasBefore) {
            throw new AppLogicException(ErrorCode.INVALID_ORDER_CURSOR,
                    "after and before cannot both be supplied");
        }

        int cappedSize = Math.min(size, PageableSanitizer.MAX_PAGE_SIZE);
        int probeLimit = cappedSize + 1;

        if (hasAfter) {
            UUID cursorId = OrderCursor.decode(afterCursor).id();
            List<Order> rows = orderRepository.findNextPage(cursorId, probeLimit);
            boolean hasNext = rows.size() > cappedSize;
            List<Order> content = trim(rows, cappedSize, hasNext);
            return buildPage(content, hasNext, true);
        }

        if (hasBefore) {
            UUID cursorId = OrderCursor.decode(beforeCursor).id();
            List<Order> ascendingRows = orderRepository.findPrevPage(cursorId, probeLimit);
            boolean hasPrevious = ascendingRows.size() > cappedSize;
            List<Order> trimmedAscending = trim(ascendingRows, cappedSize, hasPrevious);
            List<Order> content = new ArrayList<>(trimmedAscending);
            Collections.reverse(content);
            return buildPage(content, true, hasPrevious);
        }

        List<Order> rows = orderRepository.findFirstPage(probeLimit);
        boolean hasNext = rows.size() > cappedSize;
        List<Order> content = trim(rows, cappedSize, hasNext);
        return buildPage(content, hasNext, false);
    }

    private List<Order> trim(List<Order> rows, int cappedSize, boolean hasExtra) {
        return hasExtra ? rows.subList(0, cappedSize) : rows;
    }

    private OrderKeysetPage buildPage(List<Order> content, boolean hasNext, boolean hasPrevious) {
        String nextCursor = (hasNext && !content.isEmpty())
                ? new OrderCursor(content.get(content.size() - 1).getId()).encode()
                : null;
        String prevCursor = (hasPrevious && !content.isEmpty())
                ? new OrderCursor(content.get(0).getId()).encode()
                : null;

        return OrderKeysetPage.builder()
                .content(content)
                .nextCursor(nextCursor)
                .prevCursor(prevCursor)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn -q -Dtest=GetOrdersPageUseCaseImplTest test`
Expected: PASS (6/6 tests green)

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/vetautet/app/application/ordersdemo src/test/java/com/vetautet/app/application/ordersdemo
git commit -m "feat(ordersdemo): add GetOrdersPageUseCase with bidirectional keyset logic"
```

---

## Task 4: JPA entity, repository, mapper, and `OrderRepositoryAdapter`

**Files:**
- Create: `src/main/java/com/vetautet/app/infrastructure/persistence/jpa/entity/OrderJpaEntity.java`
- Create: `src/main/java/com/vetautet/app/infrastructure/persistence/jpa/repository/OrderJpaRepository.java`
- Create: `src/main/java/com/vetautet/app/infrastructure/persistence/mapper/OrderEntityMapper.java`
- Create: `src/main/java/com/vetautet/app/infrastructure/persistence/jpa/adapter/OrderRepositoryAdapter.java`
- Test: `src/test/java/com/vetautet/app/infrastructure/persistence/jpa/adapter/OrderRepositoryAdapterTest.java`

**Interfaces:**
- Consumes: `Order`, `OrderStatus`, `OrderRepository` (Task 2); `PageableSanitizer.MAX_PAGE_SIZE`.
- Produces: `OrderRepositoryAdapter implements OrderRepository` — the concrete bean wired into `GetOrdersPageUseCaseImpl` (Task 3) via Spring DI at runtime.

- [ ] **Step 1: Write the failing test**

```java
package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import com.vetautet.app.infrastructure.persistence.jpa.entity.OrderJpaEntity;
import com.vetautet.app.infrastructure.persistence.jpa.repository.OrderJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.OrderEntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryAdapterTest {

    @Mock
    private OrderJpaRepository orderJpaRepository;

    private OrderRepositoryAdapter adapter;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new OrderRepositoryAdapter(orderJpaRepository, new OrderEntityMapper());
    }

    private OrderJpaEntity entity() {
        return OrderJpaEntity.builder()
                .id(id)
                .orderCode("ORD-1")
                .customerName("A")
                .customerEmail("a@example.com")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ONE)
                .currency("VND")
                .quantity(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void findFirstPage_mapsEntitiesToDomain() {
        when(orderJpaRepository.findFirstPage(21)).thenReturn(List.of(entity()));

        List<Order> result = adapter.findFirstPage(21);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(id);
    }

    @Test
    void findNextPage_delegatesWithSameArguments() {
        UUID cursorId = UUID.randomUUID();
        when(orderJpaRepository.findNextPage(cursorId, 21)).thenReturn(List.of(entity()));

        adapter.findNextPage(cursorId, 21);

        verify(orderJpaRepository).findNextPage(cursorId, 21);
    }

    @Test
    void findPrevPage_delegatesWithSameArguments() {
        UUID cursorId = UUID.randomUUID();
        when(orderJpaRepository.findPrevPage(cursorId, 21)).thenReturn(List.of(entity()));

        adapter.findPrevPage(cursorId, 21);

        verify(orderJpaRepository).findPrevPage(cursorId, 21);
    }

    @Test
    void findFirstPage_limitAboveMaxQueryLimit_isClampedTo101() {
        when(orderJpaRepository.findFirstPage(101)).thenReturn(List.of());

        adapter.findFirstPage(5000);

        verify(orderJpaRepository).findFirstPage(101);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=OrderRepositoryAdapterTest test`
Expected: FAIL to compile — none of `OrderJpaEntity`, `OrderJpaRepository`, `OrderEntityMapper`, `OrderRepositoryAdapter` exist yet.

- [ ] **Step 3: Create the JPA entity**

```java
package com.vetautet.app.infrastructure.persistence.jpa.entity;

import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Maps the {@code orders} demo table. Deliberately does NOT extend
 * {@link BaseJpaEntity} - this table's audit-like columns
 * ({@code created_at}/{@code updated_at}) are plain business columns set
 * explicitly by the seeder (raw JDBC), not managed by Hibernate's
 * {@code AuditingEntityListener}. Rows are only ever written via
 * {@code OrderSeedJdbcWriter} (JDBC batch insert) and read via
 * {@link com.vetautet.app.infrastructure.persistence.jpa.repository.OrderJpaRepository}'s
 * native queries - {@code JpaRepository.save()} is never called for orders.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_code", nullable = false, unique = true, length = 40)
    private String orderCode;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 150)
    private String customerEmail;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "shipping_address", length = 255)
    private String shippingAddress;

    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

- [ ] **Step 4: Create the Spring Data repository with native keyset queries**

```java
package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.infrastructure.persistence.jpa.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Native (not Spring Data derived / not {@code Pageable}-based) keyset
 * queries over {@code orders}, keyed on {@code id} (UUIDv7) alone - see
 * docs/superpowers/specs/2026-08-21-orders-keyset-pagination-demo-design.md
 * for why no {@code created_at} tie-breaker or extra composite index is
 * needed. All three queries are pure primary-key range scans.
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    @Query(value = "SELECT * FROM orders ORDER BY id DESC LIMIT :limit", nativeQuery = true)
    List<OrderJpaEntity> findFirstPage(@Param("limit") int limit);

    @Query(value = "SELECT * FROM orders WHERE id < :cursorId ORDER BY id DESC LIMIT :limit", nativeQuery = true)
    List<OrderJpaEntity> findNextPage(@Param("cursorId") UUID cursorId, @Param("limit") int limit);

    @Query(value = "SELECT * FROM orders WHERE id > :cursorId ORDER BY id ASC LIMIT :limit", nativeQuery = true)
    List<OrderJpaEntity> findPrevPage(@Param("cursorId") UUID cursorId, @Param("limit") int limit);
}
```

- [ ] **Step 5: Create the entity mapper**

```java
package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.infrastructure.persistence.jpa.entity.OrderJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderEntityMapper {

    public Order toDomain(OrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Order.builder()
                .id(entity.getId())
                .orderCode(entity.getOrderCode())
                .customerName(entity.getCustomerName())
                .customerEmail(entity.getCustomerEmail())
                .customerPhone(entity.getCustomerPhone())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .quantity(entity.getQuantity())
                .paymentMethod(entity.getPaymentMethod())
                .shippingAddress(entity.getShippingAddress())
                .shippingCity(entity.getShippingCity())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public OrderJpaEntity toEntity(Order domain) {
        if (domain == null) {
            return null;
        }

        return OrderJpaEntity.builder()
                .id(domain.getId())
                .orderCode(domain.getOrderCode())
                .customerName(domain.getCustomerName())
                .customerEmail(domain.getCustomerEmail())
                .customerPhone(domain.getCustomerPhone())
                .status(domain.getStatus())
                .totalAmount(domain.getTotalAmount())
                .currency(domain.getCurrency())
                .quantity(domain.getQuantity())
                .paymentMethod(domain.getPaymentMethod())
                .shippingAddress(domain.getShippingAddress())
                .shippingCity(domain.getShippingCity())
                .notes(domain.getNotes())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
```

- [ ] **Step 6: Create the repository adapter**

```java
package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.repository.OrderRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.OrderJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.OrderEntityMapper;
import com.vetautet.app.shared.common.util.PageableSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Authoritative size-cap boundary for orders-demo reads (CLAUDE.md rule 1):
 * every {@code limit} is clamped here regardless of what any upstream layer
 * passed in. Callers (see {@code GetOrdersPageUseCaseImpl}) request
 * {@code size + 1} rows as a "has more" probe, so the ceiling is
 * {@code MAX_PAGE_SIZE + 1}, not {@code MAX_PAGE_SIZE} - reusing the same
 * shared constant per CLAUDE.md rule 2 rather than a second hardcoded max.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderRepositoryAdapter implements OrderRepository {

    private static final int MAX_QUERY_LIMIT = PageableSanitizer.MAX_PAGE_SIZE + 1;

    private final OrderJpaRepository orderJpaRepository;
    private final OrderEntityMapper orderEntityMapper;

    @Override
    public List<Order> findFirstPage(int limit) {
        return orderJpaRepository.findFirstPage(cap(limit)).stream()
                .map(orderEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findNextPage(UUID afterId, int limit) {
        return orderJpaRepository.findNextPage(afterId, cap(limit)).stream()
                .map(orderEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findPrevPage(UUID beforeId, int limit) {
        return orderJpaRepository.findPrevPage(beforeId, cap(limit)).stream()
                .map(orderEntityMapper::toDomain)
                .toList();
    }

    private int cap(int limit) {
        return Math.min(limit, MAX_QUERY_LIMIT);
    }
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `mvn -q -Dtest=OrderRepositoryAdapterTest test`
Expected: PASS (4/4 tests green)

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/vetautet/app/infrastructure/persistence/jpa/entity/OrderJpaEntity.java src/main/java/com/vetautet/app/infrastructure/persistence/jpa/repository/OrderJpaRepository.java src/main/java/com/vetautet/app/infrastructure/persistence/mapper/OrderEntityMapper.java src/main/java/com/vetautet/app/infrastructure/persistence/jpa/adapter/OrderRepositoryAdapter.java src/test/java/com/vetautet/app/infrastructure/persistence/jpa/adapter/OrderRepositoryAdapterTest.java
git commit -m "feat(ordersdemo): add OrderJpaEntity, native keyset queries, OrderRepositoryAdapter"
```

---

## Task 5: Liquibase migrations — `orders` table + RBAC grant

**Files:**
- Create: `src/main/resources/db/changelog/changes/017-create-orders-demo-table.sql`
- Create: `src/main/resources/db/changelog/changes/018-orders-demo-rbac.sql`
- Modify: `src/main/resources/db/changelog/db.changelog-master.yaml`

**Interfaces:**
- Consumes: nothing (no code dependency — this is schema only, but must match the exact column names/types `OrderJpaEntity` (Task 4) expects, and the exact status strings `OrderStatus` (Task 2) defines).
- Produces: the `orders` table and the `ordersdemo:orders:list` RBAC permission granted to `ADMIN`, consumed at runtime by Task 4's queries and Task 6's controller respectively.

This task is schema-only (SQL), no JUnit test — verified instead by running the app against a real Postgres and confirming Liquibase applies cleanly (documented as a manual step in Task 9). Still follow red/green discipline by checking the SQL is syntactically consistent with the entity before moving on.

- [ ] **Step 1: Create the table migration**

```sql
--liquibase formatted sql

--changeset vetautet-dev:017-create-orders-demo-table
--comment: Create the orders-demo table for the keyset-pagination experiment (docs/keyset-pagination-spec.md). id is UUIDv7, generated in application code (Uuid7Generator) - its embedded timestamp makes it monotonically increasing and unique on its own, so it doubles as the sole cursor sort key/tie-breaker. No composite (created_at, id) index is needed: the primary key btree on id already serves "WHERE id < :cursor ORDER BY id DESC" / "WHERE id > :cursor ORDER BY id ASC" as an index-seek range scan. See docs/superpowers/specs/2026-08-21-orders-keyset-pagination-demo-design.md.
CREATE TABLE orders
(
    id               UUID                     NOT NULL,
    order_code       VARCHAR(40)              NOT NULL,
    customer_name    VARCHAR(150)             NOT NULL,
    customer_email   VARCHAR(150)             NOT NULL,
    customer_phone   VARCHAR(30),
    status           VARCHAR(20)              NOT NULL,
    total_amount     NUMERIC(18, 2)           NOT NULL,
    currency         VARCHAR(3)               NOT NULL,
    quantity         INT                      NOT NULL,
    payment_method   VARCHAR(30),
    shipping_address VARCHAR(255),
    shipping_city    VARCHAR(100),
    notes            VARCHAR(500),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uk_orders_code UNIQUE (order_code),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'PAID', 'CANCELLED', 'REFUNDED', 'COMPLETED'))
);

--rollback DROP TABLE IF EXISTS orders;
```

- [ ] **Step 2: Create the RBAC migration**

Grants `ADMIN` (only) access to `GET /api/v1/orders-demo`, following the exact pattern in `014-rbac-master-data.sql`. `ADMIN`'s blanket `role_permissions` insert in `014` already ran and won't retroactively pick up a permission created later, so this migration inserts a fresh, scoped `role_permissions` row for `ADMIN` explicitly rather than relying on `014`'s `ON TRUE` join.

```sql
--liquibase formatted sql

--changeset vetautet-dev:018-orders-demo-rbac
--comment: Register GET /api/v1/orders-demo as ADMIN-only via RBAC (see 014-rbac-master-data.sql for the base seed pattern, and docs/superpowers/specs/2026-08-21-orders-keyset-pagination-demo-design.md for why this endpoint stays admin-only rather than open to the USER role)
INSERT INTO permissions (id, code, name, description, created_date, last_modified_date, created_by, last_modified_by, version)
VALUES ('90000000-0000-0000-0000-000000001501', 'ordersdemo:orders:list', 'List demo orders', 'List orders from the orders-demo keyset pagination table', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    last_modified_date = NOW(),
    last_modified_by = 'SYSTEM';

INSERT INTO endpoints (id, http_method, url_pattern, description, created_date, last_modified_date, created_by, last_modified_by, version)
VALUES ('90000000-0000-0000-0000-000000002501', 'GET', '/api/v1/orders-demo', 'OrderDemoController.listOrders', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0)
ON CONFLICT (http_method, url_pattern) DO UPDATE
SET description = EXCLUDED.description,
    last_modified_date = NOW(),
    last_modified_by = 'SYSTEM';

INSERT INTO permission_endpoints (permission_id, endpoint_id)
SELECT permission.id, endpoint.id
FROM permissions permission
JOIN endpoints endpoint ON endpoint.http_method = 'GET' AND endpoint.url_pattern = '/api/v1/orders-demo'
WHERE permission.code = 'ordersdemo:orders:list'
ON CONFLICT (permission_id, endpoint_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN portals portal ON portal.id = role.portal_id
JOIN permissions permission ON permission.code = 'ordersdemo:orders:list'
WHERE portal.code = 'A'
  AND role.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

--rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'ordersdemo:orders:list');
--rollback DELETE FROM permission_endpoints WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'ordersdemo:orders:list');
--rollback DELETE FROM endpoints WHERE (http_method, url_pattern) = ('GET', '/api/v1/orders-demo');
--rollback DELETE FROM permissions WHERE code = 'ordersdemo:orders:list';
```

- [ ] **Step 3: Register both changesets in the master changelog**

Edit `src/main/resources/db/changelog/db.changelog-master.yaml`, appending after the `016-add-departure-search-index.sql` include:

```yaml
  - include:
      file: db/changelog/changes/017-create-orders-demo-table.sql
  - include:
      file: db/changelog/changes/018-orders-demo-rbac.sql
```

- [ ] **Step 4: Sanity-check column/status alignment**

Confirm by inspection (no automated test here): the 15 `CREATE TABLE` columns exactly match `OrderJpaEntity`'s `@Column(name=...)` values from Task 4, and the `ck_orders_status` list exactly matches `OrderStatus.values()` from Task 2 (`PENDING, CONFIRMED, PAID, CANCELLED, REFUNDED, COMPLETED`).

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/changelog/changes/017-create-orders-demo-table.sql src/main/resources/db/changelog/changes/018-orders-demo-rbac.sql src/main/resources/db/changelog/db.changelog-master.yaml
git commit -m "feat(ordersdemo): add orders table migration and ADMIN-only RBAC grant"
```

---

## Task 6: Presentation layer — DTOs, mapper, `OrderDemoController`

**Files:**
- Create: `src/main/java/com/vetautet/app/presentation/rest/dto/response/OrderResponse.java`
- Create: `src/main/java/com/vetautet/app/presentation/rest/dto/response/OrderKeysetPageResponse.java`
- Create: `src/main/java/com/vetautet/app/presentation/rest/mapper/OrderPresentationMapper.java`
- Create: `src/main/java/com/vetautet/app/presentation/rest/controller/v1/OrderDemoController.java`
- Test: `src/test/java/com/vetautet/app/presentation/rest/controller/v1/OrderDemoControllerTest.java`

**Interfaces:**
- Consumes: `GetOrdersPageUseCase`, `OrderKeysetPage` (Task 3); `Order` (Task 2); `PageableSanitizer.MAX_PAGE_SIZE`; `BaseResponse` (existing).
- Produces: `GET /api/v1/orders-demo?after=&before=&size=` HTTP endpoint.

- [ ] **Step 1: Write the failing test**

```java
package com.vetautet.app.presentation.rest.controller.v1;

import com.vetautet.app.application.ordersdemo.dto.OrderKeysetPage;
import com.vetautet.app.application.ordersdemo.port.input.GetOrdersPageUseCase;
import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import com.vetautet.app.presentation.rest.dto.response.OrderKeysetPageResponse;
import com.vetautet.app.presentation.rest.mapper.OrderPresentationMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDemoControllerTest {

    @Mock
    private GetOrdersPageUseCase getOrdersPageUseCase;

    @Mock
    private HttpServletRequest httpRequest;

    private OrderDemoController controller;

    @BeforeEach
    void setUp() {
        controller = new OrderDemoController(getOrdersPageUseCase, new OrderPresentationMapper());
        when(httpRequest.getRequestURI()).thenReturn("/api/v1/orders-demo");
    }

    private Order order() {
        return Order.builder()
                .id(UUID.randomUUID())
                .orderCode("ORD-1")
                .customerName("A")
                .customerEmail("a@example.com")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .currency("VND")
                .quantity(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void listOrders_delegatesToUseCaseAndMapsResult() {
        OrderKeysetPage page = OrderKeysetPage.builder()
                .content(List.of(order()))
                .nextCursor("next")
                .prevCursor(null)
                .hasNext(true)
                .hasPrevious(false)
                .build();
        when(getOrdersPageUseCase.execute(null, null, 20)).thenReturn(page);

        ResponseEntity<BaseResponse<OrderKeysetPageResponse>> response =
                controller.listOrders(null, null, 20, httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        OrderKeysetPageResponse payload = response.getBody().getPayload();
        assertThat(payload.getContent()).hasSize(1);
        assertThat(payload.isHasNext()).isTrue();
        assertThat(payload.getNextCursor()).isEqualTo("next");
    }

    @Test
    void listOrders_sizeAboveMax_isCappedTo100BeforeCallingUseCase() {
        when(getOrdersPageUseCase.execute(eq(null), eq(null), anyInt()))
                .thenReturn(OrderKeysetPage.builder().content(List.of()).hasNext(false).hasPrevious(false).build());

        controller.listOrders(null, null, 999, httpRequest);

        verify(getOrdersPageUseCase).execute(null, null, 100);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=OrderDemoControllerTest test`
Expected: FAIL to compile — none of `OrderResponse`, `OrderKeysetPageResponse`, `OrderPresentationMapper`, `OrderDemoController` exist yet.

- [ ] **Step 3: Create the response DTOs**

```java
package com.vetautet.app.presentation.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID id;
    private String orderCode;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
    private Integer quantity;
    private String paymentMethod;
    private String shippingAddress;
    private String shippingCity;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
```

```java
package com.vetautet.app.presentation.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderKeysetPageResponse {
    private List<OrderResponse> content;
    private String nextCursor;
    private String prevCursor;
    private boolean hasNext;
    private boolean hasPrevious;
}
```

- [ ] **Step 4: Create the presentation mapper**

```java
package com.vetautet.app.presentation.rest.mapper;

import com.vetautet.app.application.ordersdemo.dto.OrderKeysetPage;
import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.presentation.rest.dto.response.OrderKeysetPageResponse;
import com.vetautet.app.presentation.rest.dto.response.OrderResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderPresentationMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .quantity(order.getQuantity())
                .paymentMethod(order.getPaymentMethod())
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderKeysetPageResponse toPageResponse(OrderKeysetPage page) {
        return OrderKeysetPageResponse.builder()
                .content(page.content().stream().map(this::toResponse).toList())
                .nextCursor(page.nextCursor())
                .prevCursor(page.prevCursor())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
```

- [ ] **Step 5: Create the controller**

```java
package com.vetautet.app.presentation.rest.controller.v1;

import com.vetautet.app.application.ordersdemo.dto.OrderKeysetPage;
import com.vetautet.app.application.ordersdemo.port.input.GetOrdersPageUseCase;
import com.vetautet.app.presentation.config.RequireBearerAuth;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import com.vetautet.app.presentation.rest.dto.response.OrderKeysetPageResponse;
import com.vetautet.app.presentation.rest.mapper.OrderPresentationMapper;
import com.vetautet.app.shared.common.util.PageableSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo endpoint for keyset (cursor) pagination over a simulated {@code orders}
 * table (docs/keyset-pagination-spec.md). Sequential next/previous
 * navigation only - there is intentionally no {@code page} parameter.
 * ADMIN-only (see RBAC migration 018-orders-demo-rbac.sql) in addition to
 * {@link RequireBearerAuth}.
 */
@RestController
@RequestMapping("/api/v1/orders-demo")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders Demo", description = "Keyset pagination demo over a simulated orders table")
@RequireBearerAuth
@SecurityRequirement(name = "bearer-jwt")
public class OrderDemoController {

    private final GetOrdersPageUseCase getOrdersPageUseCase;
    private final OrderPresentationMapper mapper;

    @GetMapping
    @Operation(summary = "List orders (keyset pagination)",
            description = "Sequential next/previous pagination only - no jump-to-page. Supply at most one of after/before.")
    public ResponseEntity<BaseResponse<OrderKeysetPageResponse>> listOrders(
            @Parameter(description = "Cursor for the next page") @RequestParam(required = false) String after,
            @Parameter(description = "Cursor for the previous page") @RequestParam(required = false) String before,
            @Parameter(description = "Page size (capped at " + PageableSanitizer.MAX_PAGE_SIZE + ")")
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        int safeSize = Math.min(size, PageableSanitizer.MAX_PAGE_SIZE);
        log.info("Listing orders-demo - after: {}, before: {}, size: {}", after, before, safeSize);

        OrderKeysetPage page = getOrdersPageUseCase.execute(after, before, safeSize);
        OrderKeysetPageResponse response = mapper.toPageResponse(page);

        BaseResponse<OrderKeysetPageResponse> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(), "Orders retrieved successfully", response, httpRequest.getRequestURI());

        log.info("Returned {} orders, hasNext={}, hasPrevious={}",
                response.getContent().size(), response.isHasNext(), response.isHasPrevious());
        return ResponseEntity.ok(baseResponse);
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn -q -Dtest=OrderDemoControllerTest test`
Expected: PASS (2/2 tests green)

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/vetautet/app/presentation/rest/dto/response/OrderResponse.java src/main/java/com/vetautet/app/presentation/rest/dto/response/OrderKeysetPageResponse.java src/main/java/com/vetautet/app/presentation/rest/mapper/OrderPresentationMapper.java src/main/java/com/vetautet/app/presentation/rest/controller/v1/OrderDemoController.java src/test/java/com/vetautet/app/presentation/rest/controller/v1/OrderDemoControllerTest.java
git commit -m "feat(ordersdemo): add GET /api/v1/orders-demo controller"
```

---

## Task 7: `FakeOrderDataFactory` + `OrderBulkWriter` port + `OrderSeedJdbcWriter`

**Files:**
- Create: `src/main/java/com/vetautet/app/application/ordersdemo/usecase/FakeOrderDataFactory.java`
- Test: `src/test/java/com/vetautet/app/application/ordersdemo/usecase/FakeOrderDataFactoryTest.java`
- Create: `src/main/java/com/vetautet/app/application/ordersdemo/port/output/OrderBulkWriter.java`
- Create: `src/main/java/com/vetautet/app/infrastructure/ordersdemo/seed/OrderSeedJdbcWriter.java`
- Test: `src/test/java/com/vetautet/app/infrastructure/ordersdemo/seed/OrderSeedJdbcWriterTest.java`

**Interfaces:**
- Consumes: `Order`, `OrderStatus` (Task 2).
- Produces: `FakeOrderDataFactory.generate(UUID id, Instant createdAt)` → `Order`; `OrderBulkWriter.insertBatch(List<Order> orders)` → `int` (rows attempted); `OrderSeedJdbcWriter implements OrderBulkWriter`. Task 8 depends on both exact signatures.

**Note on placement:** `FakeOrderDataFactory` lives in the **application** layer, not infrastructure — it has zero framework/DB dependency (just `java.util.Random` and in-memory arrays), and `SeedOrdersUseCaseImpl` (Task 8, application layer) needs to call it directly. Putting it in `infrastructure` would make an application-layer class depend on infrastructure, inverting the hexagonal dependency direction CLAUDE.md mandates.

- [ ] **Step 1: Write the failing test for `FakeOrderDataFactory`**

```java
package com.vetautet.app.application.ordersdemo.usecase;

import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FakeOrderDataFactoryTest {

    private final FakeOrderDataFactory factory = new FakeOrderDataFactory();

    @Test
    void generate_usesGivenIdAndCreatedAt() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-06-01T00:00:00Z");

        Order order = factory.generate(id, createdAt);

        assertThat(order.getId()).isEqualTo(id);
        assertThat(order.getCreatedAt()).isEqualTo(createdAt);
        assertThat(order.getUpdatedAt()).isEqualTo(createdAt);
    }

    @Test
    void generate_populatesAllRequiredFieldsNonBlank() {
        Order order = factory.generate(UUID.randomUUID(), Instant.now());

        assertThat(order.getOrderCode()).isNotBlank().contains("ORD-");
        assertThat(order.getCustomerName()).isNotBlank();
        assertThat(order.getCustomerEmail()).isNotBlank().contains("@");
        assertThat(order.getCustomerPhone()).isNotBlank();
        assertThat(order.getStatus()).isIn((Object[]) OrderStatus.values());
        assertThat(order.getTotalAmount().signum()).isPositive();
        assertThat(order.getCurrency()).isNotBlank();
        assertThat(order.getQuantity()).isPositive();
        assertThat(order.getPaymentMethod()).isNotBlank();
        assertThat(order.getShippingAddress()).isNotBlank();
        assertThat(order.getShippingCity()).isNotBlank();
    }

    @Test
    void generate_calledTwice_producesDifferentOrderCodesForDifferentIds() {
        Order first = factory.generate(UUID.randomUUID(), Instant.now());
        Order second = factory.generate(UUID.randomUUID(), Instant.now());

        assertThat(first.getOrderCode()).isNotEqualTo(second.getOrderCode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=FakeOrderDataFactoryTest test`
Expected: FAIL to compile — `FakeOrderDataFactory` doesn't exist yet.

- [ ] **Step 3: Implement `FakeOrderDataFactory`**

```java
package com.vetautet.app.application.ordersdemo.usecase;

import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

/**
 * Produces realistic-looking, non-sensitive synthetic {@link Order} rows for
 * the dev seeder (see {@code SeedOrdersUseCaseImpl}). No external data
 * source - small in-memory sample arrays, no new Faker-style dependency.
 */
@Component
public class FakeOrderDataFactory {

    private static final String[] FIRST_NAMES = {"An", "Binh", "Chi", "Dung", "Giang", "Hoa", "Khanh", "Lan", "Minh", "Nga"};
    private static final String[] LAST_NAMES = {"Nguyen", "Tran", "Le", "Pham", "Hoang", "Vu", "Dang", "Bui", "Do", "Ho"};
    private static final String[] CITIES = {"Ha Noi", "Ho Chi Minh", "Da Nang", "Hai Phong", "Can Tho", "Nha Trang", "Hue", "Vinh", "Bien Hoa", "Vung Tau"};
    private static final String[] PAYMENT_METHODS = {"CREDIT_CARD", "BANK_TRANSFER", "COD", "E_WALLET"};
    private static final String[] CURRENCIES = {"VND", "USD"};
    private static final OrderStatus[] STATUSES = OrderStatus.values();

    private final Random random = new Random();

    public Order generate(UUID id, Instant createdAt) {
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String customerName = lastName + " " + firstName;
        String city = CITIES[random.nextInt(CITIES.length)];
        String emailLocalPart = (firstName + "." + lastName).toLowerCase().replace(" ", "");

        return Order.builder()
                .id(id)
                .orderCode("ORD-" + id.toString().substring(0, 8).toUpperCase())
                .customerName(customerName)
                .customerEmail(emailLocalPart + "@example.com")
                .customerPhone("09" + String.format("%08d", random.nextInt(100_000_000)))
                .status(STATUSES[random.nextInt(STATUSES.length)])
                .totalAmount(BigDecimal.valueOf(10_000 + random.nextInt(4_990_000)).setScale(2, java.math.RoundingMode.UNNECESSARY))
                .currency(CURRENCIES[random.nextInt(CURRENCIES.length)])
                .quantity(1 + random.nextInt(5))
                .paymentMethod(PAYMENT_METHODS[random.nextInt(PAYMENT_METHODS.length)])
                .shippingAddress((10 + random.nextInt(990)) + " Le Loi Street")
                .shippingCity(city)
                .notes(null)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=FakeOrderDataFactoryTest test`
Expected: PASS (3/3 tests green)

- [ ] **Step 5: Write the failing test for `OrderSeedJdbcWriter`**

```java
package com.vetautet.app.infrastructure.ordersdemo.seed;

import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSeedJdbcWriterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private OrderSeedJdbcWriter writer;

    @BeforeEach
    void setUp() {
        writer = new OrderSeedJdbcWriter(jdbcTemplate);
    }

    private Order order(UUID id) {
        return Order.builder()
                .id(id)
                .orderCode("ORD-1")
                .customerName("A")
                .customerEmail("a@example.com")
                .customerPhone("0900000000")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .currency("VND")
                .quantity(1)
                .paymentMethod("COD")
                .shippingAddress("1 Le Loi")
                .shippingCity("Ha Noi")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void insertBatch_buildsOneJdbcBatchRowPerOrder_andReturnsInputSize() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<Order> orders = List.of(order(id1), order(id2));
        when(jdbcTemplate.batchUpdate(anyString(), anyList())).thenReturn(new int[]{1, 1});

        int inserted = writer.insertBatch(orders);

        assertThat(inserted).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> argsCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(anyString(), argsCaptor.capture());
        List<Object[]> capturedArgs = argsCaptor.getValue();
        assertThat(capturedArgs).hasSize(2);
        assertThat(capturedArgs.get(0)[0]).isEqualTo(id1);
        assertThat(capturedArgs.get(1)[0]).isEqualTo(id2);
    }

    @Test
    void insertBatch_emptyList_doesNotCallJdbcTemplate() {
        int inserted = writer.insertBatch(List.of());

        assertThat(inserted).isZero();
        org.mockito.Mockito.verifyNoInteractions(jdbcTemplate);
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `mvn -q -Dtest=OrderSeedJdbcWriterTest test`
Expected: FAIL to compile — `OrderBulkWriter`, `OrderSeedJdbcWriter` don't exist yet.

- [ ] **Step 7: Create the output port**

```java
package com.vetautet.app.application.ordersdemo.port.output;

import com.vetautet.app.domain.ordersdemo.model.Order;

import java.util.List;

/** Output port for bulk-writing seed data, bypassing JPA for throughput. */
public interface OrderBulkWriter {
    /** @return number of rows attempted (see implementation notes on why this isn't a driver-reported affected-row count) */
    int insertBatch(List<Order> orders);
}
```

- [ ] **Step 8: Implement `OrderSeedJdbcWriter`**

```java
package com.vetautet.app.infrastructure.ordersdemo.seed;

import com.vetautet.app.application.ordersdemo.port.output.OrderBulkWriter;
import com.vetautet.app.domain.ordersdemo.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;

/**
 * Raw JDBC batch insert for seed data - deliberately bypasses
 * {@code OrderJpaRepository}/Hibernate. 2,000,000 rows through a JPA
 * persistence context would grow unbounded memory and slow down with
 * dirty-checking overhead; a plain batched {@code PreparedStatement} does
 * not have that cost.
 */
@Component
@RequiredArgsConstructor
public class OrderSeedJdbcWriter implements OrderBulkWriter {

    private static final String INSERT_SQL = """
            INSERT INTO orders
                (id, order_code, customer_name, customer_email, customer_phone, status,
                 total_amount, currency, quantity, payment_method, shipping_address,
                 shipping_city, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int insertBatch(List<Order> orders) {
        if (orders.isEmpty()) {
            return 0;
        }

        List<Object[]> batchArgs = orders.stream().map(this::toArgs).toList();
        // Postgres' JDBC driver commonly reports Statement.SUCCESS_NO_INFO (-2)
        // per row for batched inserts rather than an exact affected-row count,
        // so summing jdbcTemplate.batchUpdate()'s result isn't reliable here -
        // the input size is the accurate "rows attempted" count.
        jdbcTemplate.batchUpdate(INSERT_SQL, batchArgs);
        return orders.size();
    }

    private Object[] toArgs(Order order) {
        return new Object[]{
                order.getId(),
                order.getOrderCode(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getQuantity(),
                order.getPaymentMethod(),
                order.getShippingAddress(),
                order.getShippingCity(),
                order.getNotes(),
                Timestamp.from(order.getCreatedAt()),
                Timestamp.from(order.getUpdatedAt())
        };
    }
}
```

- [ ] **Step 9: Run test to verify it passes**

Run: `mvn -q -Dtest=OrderSeedJdbcWriterTest test`
Expected: PASS (2/2 tests green)

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/vetautet/app/application/ordersdemo/usecase/FakeOrderDataFactory.java src/test/java/com/vetautet/app/application/ordersdemo/usecase/FakeOrderDataFactoryTest.java src/main/java/com/vetautet/app/application/ordersdemo/port/output/OrderBulkWriter.java src/main/java/com/vetautet/app/infrastructure/ordersdemo/seed/OrderSeedJdbcWriter.java src/test/java/com/vetautet/app/infrastructure/ordersdemo/seed/OrderSeedJdbcWriterTest.java
git commit -m "feat(ordersdemo): add fake data factory and JDBC batch seed writer"
```

---

## Task 8: `SeedOrdersUseCase` + `OrderSeedController`

**Files:**
- Create: `src/main/java/com/vetautet/app/application/ordersdemo/port/input/SeedOrdersUseCase.java`
- Create: `src/main/java/com/vetautet/app/application/ordersdemo/usecase/SeedOrdersUseCaseImpl.java`
- Test: `src/test/java/com/vetautet/app/application/ordersdemo/usecase/SeedOrdersUseCaseImplTest.java`
- Create: `src/main/java/com/vetautet/app/presentation/rest/controller/internal/OrderSeedController.java`
- Test: `src/test/java/com/vetautet/app/presentation/rest/controller/internal/OrderSeedControllerTest.java`
- Modify: `src/main/java/com/vetautet/app/presentation/config/SecurityConfig.java`

**Interfaces:**
- Consumes: `Order` (Task 2); `OrderBulkWriter`, `FakeOrderDataFactory` (Task 7); `Uuid7Generator` (Task 1).
- Produces: `SeedOrdersUseCase.execute(int count)` → `int`; `POST /internal/orders-demo/seed?count=` HTTP endpoint, active only under `@Profile({"dev", "local"})`.

- [ ] **Step 1: Write the failing test for the use case**

```java
package com.vetautet.app.application.ordersdemo.usecase;

import com.vetautet.app.application.ordersdemo.port.output.OrderBulkWriter;
import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeedOrdersUseCaseImplTest {

    @Mock
    private OrderBulkWriter orderBulkWriter;

    @Mock
    private FakeOrderDataFactory fakeOrderDataFactory;

    private SeedOrdersUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SeedOrdersUseCaseImpl(orderBulkWriter, fakeOrderDataFactory);
        when(fakeOrderDataFactory.generate(org.mockito.ArgumentMatchers.any(UUID.class), org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenAnswer(invocation -> Order.builder()
                        .id(invocation.getArgument(0))
                        .createdAt(invocation.getArgument(1))
                        .orderCode("ORD-X")
                        .customerName("A")
                        .customerEmail("a@example.com")
                        .status(OrderStatus.PENDING)
                        .totalAmount(BigDecimal.ONE)
                        .currency("VND")
                        .quantity(1)
                        .build());
        when(orderBulkWriter.insertBatch(anyList())).thenAnswer(invocation -> ((List<?>) invocation.getArgument(0)).size());
    }

    @Test
    void execute_countLessThanBatchSize_insertsOneBatchOfExactCount() {
        int inserted = useCase.execute(1200);

        assertThat(inserted).isEqualTo(1200);
        ArgumentCaptor<List<Order>> captor = ArgumentCaptor.forClass(List.class);
        verify(orderBulkWriter, times(1)).insertBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(1200);
    }

    @Test
    void execute_countSpanningMultipleBatches_insertsFullBatchesPlusRemainder() {
        int inserted = useCase.execute(12_000);

        assertThat(inserted).isEqualTo(12_000);
        verify(orderBulkWriter, times(3)).insertBatch(anyList());
    }

    @Test
    void execute_generatesStrictlyIncreasingTimestampsAcrossRows() {
        useCase.execute(3);

        ArgumentCaptor<Instant> timestampCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(fakeOrderDataFactory, times(3)).generate(org.mockito.ArgumentMatchers.any(UUID.class), timestampCaptor.capture());
        List<Instant> timestamps = timestampCaptor.getAllValues();
        assertThat(timestamps.get(0)).isBefore(timestamps.get(1));
        assertThat(timestamps.get(1)).isBefore(timestamps.get(2));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=SeedOrdersUseCaseImplTest test`
Expected: FAIL to compile — `SeedOrdersUseCaseImpl` doesn't exist yet.

- [ ] **Step 3: Create the input port**

```java
package com.vetautet.app.application.ordersdemo.port.input;

/** Bulk-seeds the orders-demo table with synthetic rows. Dev/local use only. */
public interface SeedOrdersUseCase {
    /** @return number of rows attempted to insert */
    int execute(int count);
}
```

- [ ] **Step 4: Implement the use case**

```java
package com.vetautet.app.application.ordersdemo.usecase;

import com.vetautet.app.application.ordersdemo.port.input.SeedOrdersUseCase;
import com.vetautet.app.application.ordersdemo.port.output.OrderBulkWriter;
import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.shared.common.util.Uuid7Generator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Spreads {@code count} synthetic orders' {@code created_at} evenly across
 * the last {@link #SEED_SPAN_DAYS} days, strictly increasing row-by-row, and
 * mints each row's UUIDv7 {@code id} from that same timestamp
 * ({@link Uuid7Generator#generate(Instant)}) - so {@code id} ordering matches
 * {@code created_at} ordering exactly, the same way real insert-time
 * UUIDv7 generation would.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeedOrdersUseCaseImpl implements SeedOrdersUseCase {

    private static final int BATCH_SIZE = 5000;
    private static final int SEED_SPAN_DAYS = 180;
    private static final int PROGRESS_LOG_INTERVAL = 100_000;

    private final OrderBulkWriter orderBulkWriter;
    private final FakeOrderDataFactory fakeOrderDataFactory;

    @Override
    public int execute(int count) {
        Instant baseTime = Instant.now().minus(SEED_SPAN_DAYS, ChronoUnit.DAYS);
        long spanMillis = Duration.ofDays(SEED_SPAN_DAYS).toMillis();

        int totalInserted = 0;
        List<Order> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < count; i++) {
            Instant createdAt = baseTime.plusMillis(i * spanMillis / count);
            UUID id = Uuid7Generator.generate(createdAt);
            batch.add(fakeOrderDataFactory.generate(id, createdAt));

            if (batch.size() == BATCH_SIZE) {
                totalInserted += orderBulkWriter.insertBatch(batch);
                batch.clear();
                if (totalInserted % PROGRESS_LOG_INTERVAL == 0) {
                    log.info("Seeded {} / {} orders", totalInserted, count);
                }
            }
        }
        if (!batch.isEmpty()) {
            totalInserted += orderBulkWriter.insertBatch(batch);
        }

        log.info("Finished seeding {} orders", totalInserted);
        return totalInserted;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -Dtest=SeedOrdersUseCaseImplTest test`
Expected: PASS (3/3 tests green)

- [ ] **Step 6: Write the failing test for `OrderSeedController`**

```java
package com.vetautet.app.presentation.rest.controller.internal;

import com.vetautet.app.application.ordersdemo.port.input.SeedOrdersUseCase;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSeedControllerTest {

    @Mock
    private SeedOrdersUseCase seedOrdersUseCase;

    @Mock
    private HttpServletRequest httpRequest;

    private OrderSeedController controller;

    @BeforeEach
    void setUp() {
        controller = new OrderSeedController(seedOrdersUseCase);
        when(httpRequest.getRequestURI()).thenReturn("/internal/orders-demo/seed");
    }

    @Test
    void seed_delegatesToUseCase_andReturnsInsertedCount() {
        when(seedOrdersUseCase.execute(2_000_000)).thenReturn(2_000_000);

        ResponseEntity<BaseResponse<Integer>> response = controller.seed(2_000_000, httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getPayload()).isEqualTo(2_000_000);
    }
}
```

- [ ] **Step 7: Run test to verify it fails**

Run: `mvn -q -Dtest=OrderSeedControllerTest test`
Expected: FAIL to compile — `OrderSeedController` doesn't exist yet.

- [ ] **Step 8: Implement `OrderSeedController`**

```java
package com.vetautet.app.presentation.rest.controller.internal;

import com.vetautet.app.application.ordersdemo.port.input.SeedOrdersUseCase;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only bulk seeder for the orders-demo table. Only exists as a Spring
 * bean under the {@code dev}/{@code local} profiles - absent from the bean
 * graph (and therefore unreachable) in any other profile, including
 * production, regardless of routing/security configuration. Reachability in
 * dev/local additionally requires {@code /internal/orders-demo/**} to be in
 * {@code SecurityConfig}'s permitAll matchers (see that class) - safety here
 * comes from profile exclusion, not from auth.
 */
@RestController
@RequestMapping("/internal/orders-demo")
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "local"})
@Tag(name = "Orders Demo Seed", description = "Dev-only bulk seeder for the orders-demo table")
public class OrderSeedController {

    private final SeedOrdersUseCase seedOrdersUseCase;

    @PostMapping("/seed")
    @Operation(summary = "Bulk-seed synthetic orders", description = "Dev/local only. Not exposed in production.")
    public ResponseEntity<BaseResponse<Integer>> seed(
            @RequestParam(defaultValue = "2000000") int count,
            HttpServletRequest httpRequest) {
        log.info("Seeding {} synthetic orders", count);
        int inserted = seedOrdersUseCase.execute(count);

        BaseResponse<Integer> response = BaseResponse.success(
                HttpStatus.OK.value(), "Seed completed", inserted, httpRequest.getRequestURI());
        return ResponseEntity.ok(response);
    }
}
```

- [ ] **Step 9: Run test to verify it passes**

Run: `mvn -q -Dtest=OrderSeedControllerTest test`
Expected: PASS (1/1 tests green)

- [ ] **Step 10: Add `/internal/orders-demo/**` to `SecurityConfig`'s permitAll matchers**

Edit `src/main/java/com/vetautet/app/presentation/config/SecurityConfig.java`, in the `.authorizeHttpRequests` block, right after the Swagger/OpenAPI `permitAll` block and before the actuator block:

```java
                        // Swagger/OpenAPI documentation - public access
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**")
                        .permitAll()

                        // Dev-only orders-demo seeder - the OrderSeedController bean only
                        // exists under the dev/local profiles (@Profile), so this matcher
                        // is unreachable in any other profile including production; safety
                        // comes from profile exclusion, not from this permitAll.
                        .requestMatchers("/internal/orders-demo/**")
                        .permitAll()

                        // Actuator endpoints - public access (consider restricting in production)
```

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/vetautet/app/application/ordersdemo/port/input/SeedOrdersUseCase.java src/main/java/com/vetautet/app/application/ordersdemo/usecase/SeedOrdersUseCaseImpl.java src/test/java/com/vetautet/app/application/ordersdemo/usecase/SeedOrdersUseCaseImplTest.java src/main/java/com/vetautet/app/presentation/rest/controller/internal/OrderSeedController.java src/test/java/com/vetautet/app/presentation/rest/controller/internal/OrderSeedControllerTest.java src/main/java/com/vetautet/app/presentation/config/SecurityConfig.java
git commit -m "feat(ordersdemo): add dev-only bulk seed endpoint"
```

---

## Task 9: Full build verification + manual smoke-test instructions

**Files:** none created/modified — verification only.

- [ ] **Step 1: Run the full test suite**

Run: `mvn -q test`
Expected: BUILD SUCCESS, all tests pass (including the ~25 new tests added across Tasks 1-8), no regressions in existing tests.

- [ ] **Step 2: Run a full compile to catch anything the per-class test runs missed**

Run: `mvn -q clean compile test-compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Document manual smoke-test steps (requires a running Postgres + the app started with an ADMIN JWT)**

This step is not automatable in this environment (no live Postgres instance here) — record the steps for the user to run locally:

```bash
# 1. Start the app with the dev profile so the seed endpoint exists:
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 2. Liquibase applies 017/018 automatically on startup. Seed a small batch first
#    to sanity-check before committing to the full 2,000,000-row run:
curl -X POST "http://localhost:8080/internal/orders-demo/seed?count=1000"

# 3. Fetch the first page as an ADMIN-authenticated caller (replace $ADMIN_JWT):
curl -H "Authorization: Bearer $ADMIN_JWT" "http://localhost:8080/api/v1/orders-demo?size=5"
#    -> note the "nextCursor" in the response payload

# 4. Page forward:
curl -H "Authorization: Bearer $ADMIN_JWT" "http://localhost:8080/api/v1/orders-demo?after=<nextCursor>&size=5"
#    -> note this page's "prevCursor"

# 5. Page back and confirm it reproduces step 3's page exactly:
curl -H "Authorization: Bearer $ADMIN_JWT" "http://localhost:8080/api/v1/orders-demo?before=<prevCursor>&size=5"

# 6. Once satisfied, run the full 2,000,000-row seed (takes a few minutes):
curl -X POST "http://localhost:8080/internal/orders-demo/seed?count=2000000"
```

- [ ] **Step 4: Report results**

Summarize to the user: test suite pass/fail counts, any deviations from the plan discovered during implementation, and the above manual verification steps for them to run against a live Postgres instance.
