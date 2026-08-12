# Detail Design - Check User Availability

## 1. Overview

- Use case: Check Username/Email Availability
- Business goal: Cho form đăng ký một hint real-time (gõ tới đâu check tới đâu) về việc username/email đã bị dùng hay chưa, tránh phải submit rồi mới biết trùng.
- In-scope: check tồn tại theo `USERNAME` hoặc `EMAIL`, best-effort (không cam kết chính xác 100%).
- Out-of-scope: đăng ký thực sự (xem `uc-auth-register`), validate format username/email (được làm ở tầng khác trước khi gọi API này).
- Đặc điểm quan trọng: endpoint **public, không cần JWT**, nên phải chống được user-enumeration bằng rate limit theo IP (không phải theo tài khoản).

## 2. Layer Mapping (Clean Architecture)

- Presentation:
    - `UserAvailabilityController#checkAvailability` (`presentation/rest/controller/v1`)
    - `AvailabilityResponse` (`presentation/rest/dto/response`)
    - `@RateLimit(scope = "availability")` (`presentation/config/ratelimit`)
- Application:
    - Input port: `CheckUserAvailabilityUseCase` (`application/user/port/input`)
    - Use case impl: `CheckUserAvailabilityUseCaseImpl` (`application/user/usecase`)
    - Output port: `UserAvailabilityProbe` (`application/user/port/output`)
    - DTO: `AvailabilityCheckType` (enum `USERNAME`/`EMAIL`), `AvailabilityResult`
- Domain:
    - `UserRepository#existsByUsernameIgnoreCase` / `#existsByEmailIgnoreCase` (domain-owned interface, dùng làm fallback)
- Infrastructure:
    - `RedissonUserAvailabilityProbeAdapter` implements `UserAvailabilityProbe` - đọc 2 Bloom filter Redisson (`userUsernameBloomFilter`, `userEmailBloomFilter`) + sync-state bucket
    - `UserBloomFilterSyncService` - job sync 1 lần khi start app (`NOT_SYNCED` → `SYNCING` → `SYNCED`), không nằm trong flow request-time nhưng quyết định nhánh nào được chọn ở bước 3 dưới đây

Không có REST DTO ↔ domain mapper riêng ở use case này vì payload quá đơn giản (`String type`, `String value`) - controller build `AvailabilityResponse` trực tiếp từ `AvailabilityResult`.

## 3. Sequence Flow

### Happy path (Bloom filter đã SYNCED)

1. Client gọi `GET /api/v1/users/availability?type=username&value=john_doe` (không kèm Authorization header).
2. `SecurityConfig` cho qua vì path này nằm trong danh sách `permitAll()`.
3. `RateLimitInterceptor.preHandle()` chạy trước khi vào controller: tra `app.rate-limit.scopes.availability` (soft 20/phút, hard 200/giờ), check theo IP qua `IpRateLimiter`. Nếu bị chặn → dừng ở đây (xem mục Rate Limit Flow).
4. `UserAvailabilityController.checkAvailability`:
    - `AvailabilityCheckType.valueOf(type.trim().toUpperCase())` parse `type` thành enum.
    - Gọi `checkUserAvailabilityUseCase.execute(checkType, value)`.
5. `CheckUserAvailabilityUseCaseImpl.execute` (`@Transactional(readOnly = true)`):
    - `normalize(value)` → trim + lowercase.
    - `userAvailabilityProbe.isSynced()` → `true`.
    - `userAvailabilityProbe.mightExist(type, normalized)` → đọc `RBloomFilter.contains(...)` tương ứng (username hoặc email). Bloom filter có thể false positive, không bao giờ false negative.
    - Trả `AvailabilityResult(type, value, available = !exists)`.
6. Controller map `AvailabilityResult` → `AvailabilityResponse` (`type` lowercase, `value`, `available`).
7. Trả `200 OK` với `BaseResponse.success(response)`.

### Fallback path (Bloom filter chưa SYNCED, ví dụ app vừa mới start)

- Ở bước 5, `userAvailabilityProbe.isSynced()` → `false` → `CheckUserAvailabilityUseCaseImpl.checkDatabase()` gọi trực tiếp `UserRepository.existsByUsernameIgnoreCase` / `existsByEmailIgnoreCase` (hit DB thật, chậm hơn nhưng không sai).
- Phần còn lại của flow giống happy path.

### Rate Limit Flow (chặn ở tầng interceptor, trước cả use case)

1. `RateLimitInterceptor` resolve IP client, đọc header captcha token (`IpRateLimiter.CAPTCHA_TOKEN_HEADER`) nếu có.
2. `IpRateLimiter.check(...)` trả `RateLimitDecision`:
    - Vượt soft limit (20/phút) → yêu cầu captcha (`requiresCaptcha = true`).
    - Vượt hard limit (200/giờ) → block tạm 60 phút (`blocked = true`).
3. Nếu `decision.allowed() == false` → `RateLimitResponseWriter.writeTooManyRequests(...)` viết response `429` trực tiếp, controller/use case **không được gọi**.

## 4. Exception Flow

| Scenario                                             | Nguồn                                   | Xử lý                                                           | HTTP Code |
| ----------------------------------------------------- | ---------------------------------------- | ---------------------------------------------------------------- | --------- |
| `type` không phải `username`/`email` (case-insensitive) | `AvailabilityCheckType.valueOf(...)` throw `IllegalArgumentException` | `GlobalExceptionHandler.handleIllegalArgumentException` | 400       |
| Vượt soft/hard rate limit theo IP                    | `RateLimitInterceptor` (chạy trước controller) | `RateLimitResponseWriter.writeTooManyRequests`                    | 429       |
| `value` rỗng/null                                    | Không có validation riêng - `normalize()` coi `null` thành `""`, vẫn chạy check bình thường | -                                                                 | 200 (available tuỳ dữ liệu) |

## 5. Data Contract

### Request

`GET /api/v1/users/availability`

| Param   | Type   | Required | Ghi chú                              |
| ------- | ------ | -------- | ------------------------------------- |
| `type`  | string | yes      | `username` hoặc `email`, không phân biệt hoa/thường |
| `value` | string | yes      | Giá trị user đang gõ trên form đăng ký |

### Response (200 OK)

```json
{
  "timestamp": "2026-08-12T10:00:00Z",
  "status": 200,
  "message": "Success",
  "payload": {
    "type": "username",
    "value": "john_doe",
    "available": false
  }
}
```

`available = false` nghĩa là "khả năng cao đã bị dùng" (không phải cam kết tuyệt đối, vì Bloom filter chỉ false-positive theo một chiều).

### Response (400 Bad Request - type không hợp lệ)

```json
{
  "timestamp": "2026-08-12T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "No enum constant com.vetautet.app.application.user.dto.AvailabilityCheckType.PHONE"
}
```

### Response (429 Too Many Requests)

Viết trực tiếp bởi `RateLimitResponseWriter`, không đi qua `BaseResponse.error(...)` chuẩn của `GlobalExceptionHandler` - tham khảo `RateLimitResponseWriter` khi cần tái tạo chính xác format.

## 6. Ghi chú khi dùng doc này để gen code tương tự

Đây là ví dụ điển hình cho pattern **"public read-only check, không cần bảo mật RBAC nhưng cần chống enumeration"**:

- Controller riêng, KHÔNG gộp vào controller chính của resource (ở đây là `UserController`) - vì route này permitAll trong khi các route khác của `UserController` yêu cầu JWT + `@PreAuthorize`.
- Thêm route mới vào `SecurityConfig` permitAll list.
- Thêm 1 scope mới trong `application.yml` (`app.rate-limit.scopes.<scope>`) + `@RateLimit(scope = "...")` trên method - không cần class Java mới cho rate limit.
- Use case impl nên có 2 nhánh tốc độ (index nhanh - ví dụ cache/Bloom filter - và fallback đúng - DB) khi cần "best-effort nhưng không được sai theo chiều nguy hiểm" (ở đây: không được false negative).
- DTO input/output đơn giản (2-3 field) thì không cần mapper riêng ở application layer, controller build response trực tiếp từ result DTO.
