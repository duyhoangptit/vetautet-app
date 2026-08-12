# Thiết Kế & Kế Hoạch Triển Khai: AI Monitoring & Analysis Agent
### (Phiên bản cá nhân — Analyze & Report, không Auto-Fix)

---

## 1. Mục tiêu & Nguyên tắc thiết kế

**Mục tiêu:** Xây dựng hệ thống tự động phát hiện bất thường (API lỗi / tài nguyên hạ tầng), dùng Claude để phân tích nguyên nhân gốc, và gửi báo cáo có cấu trúc qua Telegram cho kỹ sư — **không tự động sửa lỗi hay deploy lại**.

**Nguyên tắc cốt lõi:**
- **Human-in-the-loop tuyệt đối**: AI chỉ phân tích và đề xuất, không có quyền thực thi bất kỳ hành động nào lên hệ thống.
- **Tách 2 luồng phân tích riêng biệt** theo bản chất lỗi (logic API vs hạ tầng), vì nguồn dữ liệu và cách suy luận khác nhau.
- **Không gửi dữ liệu nhạy cảm** ra ngoài — dù là bản cá nhân, vẫn tập luyện thói quen sanitize log trước khi build prompt.
- **Output có cấu trúc (JSON)** từ Claude, không phải text tự do — dễ parse, dễ audit, dễ mở rộng sang auto-fix sau này nếu muốn.

---

## 2. Kiến trúc tổng quan

```
┌─────────────────┐     ┌──────────────────┐
│   Prometheus/    │     │   ELK (Filebeat/  │
│   Alertmanager   │     │   Logstash/ES)    │
└────────┬─────────┘     └────────┬──────────┘
         │ webhook alert           │ log storage
         ▼                         │
┌──────────────────────────────────┴──────────┐
│           Middleware Engine (FastAPI)         │
│  ┌────────────┐  ┌────────────┐  ┌─────────┐ │
│  │ Alert Router│─▶│ Context     │─▶│ Sanitizer│ │
│  │ (A vs B)   │  │ Fetcher     │  │          │ │
│  └────────────┘  └────────────┘  └────┬────┘ │
│                                        ▼       │
│                              ┌──────────────┐  │
│                              │ Prompt Builder│  │
│                              └──────┬───────┘  │
└─────────────────────────────────────┼──────────┘
                                       ▼
                            ┌─────────────────┐
                            │  Claude API      │
                            │  (structured     │
                            │   JSON output)   │
                            └────────┬────────┘
                                     ▼
                            ┌─────────────────┐
                            │ Report Formatter │
                            └────────┬────────┘
                                     ▼
                            ┌─────────────────┐
                            │  Telegram Bot    │
                            │  (report only)   │
                            └─────────────────┘
                                     │
                                     ▼
                            ┌─────────────────┐
                            │ Audit Log (SQLite/│
                            │ PostgreSQL local) │
                            └─────────────────┘
```

---

## 3. Luồng A: Phân tích lỗi API (theo requestId)

### 3.1. Trigger
- Alertmanager rule: error rate spike theo `service`/`endpoint` (dựa trên metric HTTP 5xx count, hoặc log-based alert nếu dùng ElastAlert/Grafana Loki alerting).
- Alert payload cần có label: `alert_type: api_error`, `service`, `endpoint` (nếu có).

### 3.2. Context Fetcher
| Bước | Nguồn | Nội dung lấy |
|---|---|---|
| 1 | Elasticsearch | Query log lỗi trong time range của alert, filter `service.keyword` + `level: ERROR`, lấy N requestId gần nhất (N=3-5) |
| 2 | Elasticsearch | Với mỗi requestId → query full trace: tất cả log entries có cùng `requestId`, sắp xếp theo timestamp, xuyên suốt các service nếu microservices |
| 3 | (optional) | Nếu có Grafana Tempo/Jaeger tracing → lấy thêm span trace cho requestId đó |

### 3.3. Prompt Template (System Prompt)
```
Bạn là kỹ sư backend cao cấp chuyên điều tra lỗi API.
Dưới đây là full trace log của (các) request bị lỗi, theo requestId,
xuyên suốt các service liên quan.

Nhiệm vụ:
1. Xác định service/module là nguyên nhân gốc (root cause).
2. Phân loại lỗi: business_logic | validation | external_call_timeout |
   null_pointer | data_inconsistency | authentication | other.
3. Đánh giá mức độ nghiêm trọng: low | medium | high | critical.
4. Đề xuất hướng điều tra tiếp theo cho kỹ sư (KHÔNG đề xuất auto-fix).

Trả về DUY NHẤT JSON theo schema:
{
  "root_cause_service": "string",
  "error_category": "string (enum trên)",
  "severity": "string (enum trên)",
  "summary": "string, tối đa 3 câu",
  "affected_requestIds": ["string"],
  "investigation_hints": ["string", "string"]
}
```

### 3.4. Output → Telegram Report (mẫu)
```
🔴 [API ERROR] service-payment / POST /transfer
Severity: HIGH | Category: external_call_timeout

Tóm tắt: Service payment gọi sang service ledger bị timeout sau 5s,
xảy ra với 12 request trong 3 phút qua.

RequestId mẫu: abc-123, def-456
Gợi ý điều tra:
 • Kiểm tra ledger-service có đang bị nghẽn connection pool không
 • Xem lại timeout config giữa payment→ledger (hiện đang 5s)

[Xem full trace log] [Đánh dấu đã xử lý]
```
→ Đây là **report thuần**, nút bấm chỉ để đánh dấu trạng thái (ack), không trigger action nào lên hệ thống.

---

## 4. Luồng B: Phân tích tài nguyên/hạ tầng bất thường

### 4.1. Trigger
- Prometheus Alertmanager rule: `cpu_usage > 80%`, `db_connection_pool_active/max > 0.9`, `memory_usage > 80%`, v.v.
- Alert payload có label: `alert_type: resource`, `instance`, `metric_name`, `current_value`.

### 4.2. Context Fetcher
| Bước | Nguồn | Nội dung lấy |
|---|---|---|
| 1 | Prometheus `query_range` API | Trend của metric gây alert trong 15-30 phút gần nhất (tăng dần hay đột biến) |
| 2 | Prometheus | Metric liên quan cùng service (VD: pool cao → kèm query latency, active threads, GC time nếu là JVM) |
| 3 | Elasticsearch | Log ERROR/WARN của đúng service trong cùng time range (không cần requestId) |

### 4.3. Prompt Template (System Prompt)
```
Bạn là SRE (Site Reliability Engineer) cao cấp.
Dưới đây là xu hướng metric hạ tầng trong 30 phút qua và log
ERROR/WARN liên quan của service.

Nhiệm vụ:
1. Xác định nguyên nhân khả dĩ nhất của bất thường tài nguyên.
2. Đánh giá đây là sự cố tạm thời (traffic spike, batch job) hay
   dấu hiệu leak/degradation (memory leak, connection leak, deadlock).
3. Đề xuất mức độ khẩn cấp: low | medium | high | critical.
4. Liệt kê các bước kiểm tra thủ công tiếp theo (KHÔNG tự thực thi).

Trả về DUY NHẤT JSON theo schema:
{
  "metric": "string",
  "instance": "string",
  "likely_cause": "string",
  "is_transient": boolean,
  "urgency": "string (enum trên)",
  "summary": "string, tối đa 3 câu",
  "manual_check_steps": ["string", "string"]
}
```

### 4.4. Output → Telegram Report (mẫu)
```
🟠 [RESOURCE] order-service / DB Connection Pool
Urgency: MEDIUM | Transient: No

Tóm tắt: Connection pool active/max = 92%, tăng liên tục trong 20
phút, không giảm dù traffic bình thường → nghi ngờ connection leak.

Gợi ý kiểm tra:
 • Xem log slow query trong order-service
 • Kiểm tra transaction nào đang không release connection
   (thường do thiếu @Transactional hoặc try-with-resources sai)

[Xem Grafana dashboard] [Đánh dấu đã xử lý]
```

---

## 5. Thành phần kỹ thuật cần xây

| # | Thành phần | Công nghệ | Ghi chú |
|---|---|---|---|
| 1 | Alertmanager webhook receiver | FastAPI endpoint `/webhook/alert` | Nhận cả alert Prometheus (luồng B) và alert log-based nếu có (luồng A) |
| 2 | Alert Router | Python, route theo label `alert_type` | Quyết định gọi context fetcher nào |
| 3 | Prometheus Client | `requests` gọi `GET /api/v1/query_range` | Cho luồng B |
| 4 | Elasticsearch Client | `elasticsearch-py` | Query theo requestId (A) hoặc theo service+timerange (B) |
| 5 | Sanitizer | Regex rules (mask số thẻ, account, CCCD nếu log có) | Chạy trước khi build prompt, dù cá nhân vẫn nên có |
| 6 | Prompt Builder | Template theo luồng A/B (mục 3.3 và 4.3) | Ép Claude trả JSON |
| 7 | Claude Client | Anthropic SDK, `response_format` ép JSON | Có retry + validate schema |
| 8 | Report Formatter | Convert JSON → Markdown message Telegram | Emoji theo severity/urgency |
| 9 | Telegram Bot | `python-telegram-bot`, chỉ có nút "Ack", không có nút thực thi | Chat ID hardcode để bảo mật |
| 10 | Audit Log | SQLite (bản cá nhân) — lưu alert gốc, context đã fetch, output Claude, thời gian | Chuẩn bị sẵn schema để sau này dễ mở rộng lên PostgreSQL |

---

## 6. Kế hoạch triển khai (Implementation Plan)

### Giai đoạn 0 — Chuẩn bị (0.5 ngày)
- [ ] Xác nhận Prometheus Alertmanager đã config được webhook receiver tùy chỉnh.
- [ ] Xác nhận Elasticsearch có index chứa `requestId` field (kiểm tra log format hiện tại, đảm bảo log có structured field, không phải plain text).
- [ ] Tạo Telegram bot, lấy token, xác định Chat ID cá nhân.

### Giai đoạn 1 — Middleware khung sườn (1 ngày)
- [ ] Dựng FastAPI project, endpoint `/webhook/alert`.
- [ ] Alert Router phân biệt `alert_type: api_error` / `resource`.
- [ ] Log request nhận được ra console để test webhook hoạt động đúng.

### Giai đoạn 2 — Context Fetcher (1.5 ngày)
- [ ] Viết `query_elk_by_requestid()` — test bằng cách gõ tay 1 requestId có thật để verify data trả về đúng.
- [ ] Viết `query_elk_by_service_timerange()` cho luồng B.
- [ ] Viết `query_prometheus_range()` — test với 1 metric có sẵn trên Grafana.
- [ ] Viết Sanitizer (regex mask cơ bản), test với vài dòng log mẫu có chứa số thẻ/account giả.

### Giai đoạn 3 — Claude Integration (1 ngày)
- [ ] Viết 2 prompt template (A/B) theo mục 3.3, 4.3.
- [ ] Ép output JSON, validate bằng Pydantic schema.
- [ ] Test với data mẫu (copy log thật đã sanitize) để tinh chỉnh prompt cho ra JSON đúng và hữu ích.

### Giai đoạn 4 — Report & Telegram (1 ngày)
- [ ] Report Formatter: JSON → Markdown message theo mẫu mục 3.4 / 4.4.
- [ ] Tích hợp Telegram Bot gửi message, nút "Ack" (chỉ update trạng thái trong Audit Log, không gọi hệ thống nào khác).
- [ ] Test end-to-end: giả lập alert → nhận được report trên Telegram.

### Giai đoạn 5 — Audit Log & Fault Injection Test (1 ngày)
- [ ] SQLite schema: `alerts`, `context_snapshots`, `claude_analysis`, `telegram_reports`.
- [ ] Viết endpoint `/simulate-crash` hoặc script tạo alert giả cho cả 2 luồng để test full loop mà không cần đợi lỗi thật.
- [ ] Chạy thử ít nhất 5 lần mỗi luồng, review chất lượng phân tích của Claude, tinh chỉnh prompt nếu cần.

### Giai đoạn 6 — Vận hành thử & tối ưu (ongoing)
- [ ] Để chạy song song với hệ thống thật trong 1-2 tuần, so sánh phân tích của Claude với root cause thực tế bạn tự điều tra được.
- [ ] Ghi nhận case Claude phân tích sai/thiếu context → cải thiện Context Fetcher hoặc prompt.
- [ ] (Tương lai, ngoài scope hiện tại) Khi đã tin tưởng độ chính xác, mới cân nhắc thêm runbook allow-list cho vài action rủi ro thấp ở luồng B.

---

## 7. Ngoài phạm vi (Explicitly Out of Scope — bản này)
- Không auto-fix, không auto-deploy, không thực thi bất kỳ script nào lên hệ thống.
- Không tích hợp CI/CD trigger.
- Không xử lý multi-tenant / nhiều Chat ID.
- Chưa cần HA cho middleware (chạy local/single instance là đủ ở giai đoạn này).

---

## 8. Định hướng mở rộng sau này
Khi kiến trúc report-only đã chạy ổn định và bạn tin tưởng chất lượng phân tích của Claude, có thể tiến tới:
1. Thêm runbook allow-list cho luồng B (action rủi ro thấp: restart, scale) — theo đúng hướng đã thảo luận ở kiến trúc dành cho bank.
2. Tích hợp Grafana Loki thay ELK nếu quyết định migrate (đã có sẵn learning path của bạn).
3. Đưa audit log từ SQLite lên PostgreSQL để dễ query/dashboard hóa (Grafana đọc trực tiếp từ PostgreSQL).