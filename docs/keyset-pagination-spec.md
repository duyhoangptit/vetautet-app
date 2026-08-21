# Keyset Pagination cho bảng dữ liệu lớn (>1M records) — Spring Boot + JPA

## 1. Bối cảnh & Vấn đề

Bảng `orders` (hoặc bất kỳ bảng transactional nào >1 triệu bản ghi) đang dùng phân trang kiểu:

```sql
SELECT * FROM orders ORDER BY created_at DESC LIMIT 20 OFFSET :page*20
```

### Lỗi 1 — Thiếu tie-breaker
`created_at` không unique (nhiều record trùng timestamp) → `ORDER BY` không ổn định giữa các lần query → trang sau có thể trùng lặp hoặc bỏ sót record.

### Lỗi 2 — OFFSET không scale
`OFFSET` lớn buộc DB quét và loại bỏ toàn bộ record phía trước → độ phức tạp O(n), càng sâu trang càng chậm. Với >1M record, OFFSET ở trang cuối có thể mất vài giây đến hàng chục giây.

### Lỗi 3 (thường gặp trong review) — Spring Data JPA `Pageable` mặc định
```java
Pageable pageable = PageRequest.of(pageNumber, size);
```
Khi tăng `pageNumber`, Spring tự sinh `OFFSET pageNumber*size` → dính lại đúng lỗi 2 dù code "trông có vẻ chuẩn".

---

## 2. Giải pháp: Keyset (Cursor-based) Pagination

Thay vì "bỏ qua N record", dùng giá trị của record cuối trang trước làm điều kiện `WHERE` để "tiếp tục từ đó" — tận dụng index seek thay vì table/index scan.

**Nguyên tắc bắt buộc:**
- `ORDER BY` phải có tie-breaker unique (thường là `id`) đi kèm cột chính (`created_at`).
- Composite index phải khớp thứ tự `ORDER BY`.
- Cursor phải encode đủ thông tin để reconstruct điều kiện `WHERE` (thường base64 của `createdAt + id`).

---

## 3. Yêu cầu code sample cần generate

Generate code Spring Boot 3.x (Java 21) theo cấu trúc sau, áp dụng cho entity `Order`:

### 3.1. Entity
- Field bắt buộc: `id` (Long, PK), `createdAt` (Instant)
- Thêm `@Index` gợi ý trong comment hoặc Flyway/Liquibase migration cho composite index `(created_at DESC, id DESC)`

### 3.2. Repository
- Dùng `@Query` (JPQL hoặc native) — KHÔNG dùng `Pageable` với `PageRequest.of(pageNumber, size)` tăng dần.
- 2 method: `findFirstPage(Pageable pageable)` và `findNextPage(cursorCreatedAt, cursorId, Pageable pageable)`.
- Điều kiện composite: ưu tiên native query dùng row-value comparison `(created_at, id) < (?, ?)` nếu chạy PostgreSQL (nhanh hơn OR-chain). Có fallback JPQL dùng OR-chain cho DB không hỗ trợ row-value (MySQL cũ, SQL Server).

### 3.3. Cursor DTO
- `record OrderCursor(Instant createdAt, Long id)`
- Method `encode()` / static `decode(String)` dùng Base64.
- Cần xử lý an toàn khi cursor input không hợp lệ (throw `IllegalArgumentException` → map sang HTTP 400).

### 3.4. Service layer
- Method `getOrders(String cursor, int size)`.
- Query `size + 1` record để xác định `hasNext` mà không cần thêm 1 query `COUNT`.
- Trả về response object gồm: `data`, `nextCursor`, `hasNext`.

### 3.5. Controller
- `GET /orders?cursor={cursor}&size={size}`
- `cursor` optional (null = trang đầu).
- Validate `size` (default 20, max 100 — tránh client abuse).

### 3.6. Migration script
- Flyway/Liquibase script tạo index: `CREATE INDEX idx_orders_created_id ON orders(created_at DESC, id DESC);`

---

## 4. Edge cases cần AI xử lý khi generate code

1. **Trùng giá trị `created_at` + `id` ở boundary**: đảm bảo composite condition không bỏ sót/lặp record đúng ranh giới cursor.
2. **Cursor không hợp lệ / bị tamper**: decode fail → trả 400, không throw exception thô.
3. **Trang đầu tiên**: không có cursor → query riêng, không dùng `WHERE` composite.
4. **Concurrent insert/update trong lúc client đang phân trang**: keyset tự nhiên tránh được duplicate/skip vì không dựa vào vị trí tuyệt đối (khác OFFSET).
5. **Sort field khác `created_at`** (VD: `amount`, `status`): AI cần generalize pattern thành generic cursor theo field bất kỳ (nullable field cần xử lý riêng — dùng `IS NULL` kết hợp).

---

## 5. Trade-off cần note rõ trong code comment / doc sinh ra

- Keyset **không hỗ trợ** "nhảy tới trang N" (VD trang 50) — chỉ đi tuần tự next/prev.
- Nếu FE bắt buộc cần jump-to-page (VD dashboard admin), đề xuất hybrid:
  - OFFSET pagination cho UI thông thường (số trang nhỏ, ít row).
  - Keyset cho API công khai / infinite-scroll / export dữ liệu lớn.
- Với bảng banking (audit, transaction log), ưu tiên keyset mặc định vì volume luôn tăng và cần scale ổn định.

---

## 6. Output mong muốn khi đưa spec này cho AI

- Full code Java: Entity, Repository, DTO, Service, Controller, Exception handler cho cursor không hợp lệ.
- 1 file test (`@SpringBootTest` hoặc `@DataJpaTest`) cover: trang đầu, trang tiếp theo, trùng `created_at`, cursor không hợp lệ, `hasNext = false` ở trang cuối.
- Migration script tạo index.
- Ghi chú ngắn gọn (comment đầu file) nhắc lại lý do không dùng OFFSET cho bảng >1M record — để review sau này không revert nhầm về cách cũ.
