# Email Notification System Design
## Flash Sale / Peak Traffic Architecture (AWS + Spring Boot + Kafka)

Author: Solution Architect

Version: 1.0

---

# 1. Overview

Hệ thống được thiết kế để xử lý các đợt Flash Sale hoặc các khoảng thời gian có lưu lượng gửi email tăng đột biến.

Ví dụ:

- Flash Sale
- Black Friday
- Campaign Marketing
- Order Confirmation
- Voucher Distribution

Đặc điểm:

- Có thể nhận hàng trăm nghìn tới hàng triệu request trong vài phút.
- Không được làm nghẽn Kafka.
- Không được làm nghẽn API.
- Không vượt quota của AWS SES.
- Đảm bảo mọi email đều được lưu và retry nếu cần.
- Hỗ trợ theo dõi trạng thái gửi.

---

# 2. Business Requirement

Ví dụ:

Trong vòng 5 phút:

```
1,000,000 email
```

AWS SES chỉ cho phép

```
12~14 email/s
```

=> Hệ thống phải hấp thụ toàn bộ traffic trước.

Sau đó gửi dần theo quota.

---

# 3. High Level Architecture

```
                    +----------------+
                    | Business       |
                    | Services       |
                    +-------+--------+
                            |
                Kafka / REST API
                            |
                            ▼
              +-------------------------+
              | Spring Boot Ingestion   |
              +-------------------------+
                  |             |
                  |             |
          Idempotency       Validation
                  |
                  ▼
          Notification DB
                  |
                  ▼
        Transactional Outbox
                  |
                  ▼
        Outbox Publisher Job
                  |
                  ▼
         Amazon SQS Standard
                  |
                  ▼
       Dispatch Worker Cluster
                  |
                  ▼
     Distributed Rate Limiter
                  |
                  ▼
             Amazon SES
                  |
          Delivery Events
                  |
                  ▼
          SNS -> SQS -> Lambda
                  |
                  ▼
          Update Notification DB
```

---

# 4. Design Principle

## Fast In

Hệ thống phải nhận request thật nhanh.

Không gọi SES ngay khi nhận request.

API chỉ:

- validate
- persist
- commit transaction

sau đó trả kết quả.

---

## Slow Out

SES chỉ cho gửi

```
12 email/s
```

Worker sẽ gửi theo tốc độ này.

---

## Queue Buffer

SQS đóng vai trò Buffer.

Ví dụ:

```
Input

3000 msg/s

Output

12 msg/s
```

Queue sẽ hấp thụ toàn bộ traffic.

---

## Eventual Delivery

Email không cần gửi ngay lập tức.

Quan trọng nhất:

- Không mất email
- Retry được
- Theo dõi được trạng thái

---

# 5. Component Design

---

## 5.1 Kafka Consumer

Receive

```
EmailRequestedEvent
```

hoặc

REST API

```
POST /notifications/email
```

Chức năng

- validate
- deduplicate
- lưu DB
- ghi Outbox

Không gọi AWS.

---

## 5.2 Notification Database

Lưu

```
Notification
```

Ví dụ

```
notification_id

recipient

subject

template

payload

status

priority

expire_time

created_at
```

---

## 5.3 Idempotency

Unique Key

```
business_key
+
recipient
+
template
```

Hoặc

```
idempotency_key
```

Nếu duplicate

Không tạo record mới.

---

## 5.4 Transactional Outbox

Sau khi transaction commit

Outbox record được tạo.

Ví dụ

```
status

QUEUED

PROCESSING

DONE
```

Publisher sẽ đọc Outbox.

---

## 5.5 Outbox Publisher

Batch

Ví dụ

```
100

200

500 records
```

Publish vào

```
Amazon SQS
```

Không gọi SES.

---

## 5.6 Amazon SQS

Vai trò

Burst Buffer

Ví dụ

```
Input

3000/s

Output

12/s
```

Queue có thể lưu

```
1 triệu messages
```

hoặc nhiều hơn.

---

## 5.7 Dispatch Worker

Worker chạy trên

- ECS
- Kubernetes
- EC2

Long Polling

```
ReceiveMessage
```

Batch

Ví dụ

```
10

20

50
```

---

## 5.8 Distributed Rate Limiter

Mọi worker dùng chung.

Ví dụ

```
12 token/s
```

Worker phải lấy token trước khi gọi SES.

Không có token

↓

chờ.

---

## 5.9 Amazon SES

Worker chỉ gọi

```
SendEmail
```

hoặc

```
SendBulkEmail
```

Không retry ngay.

---

## 5.10 Retry Scheduler

Retry khi

```
429

5xx

timeout

network error
```

Backoff

```
1m

2m

5m

15m

30m
```

Thêm

```
jitter
```

để tránh Retry Storm.

---

## 5.11 DLQ

Sau

```
maxReceiveCount
```

↓

DLQ

Ví dụ

```
5 lần
```

Manual

```
Redrive
```

---

## 5.12 SNS

SES publish

```
Delivery

Bounce

Complaint

Reject
```

↓

SNS

↓

SQS

↓

Lambda

↓

Update DB

---

# 6. Notification Status

```
CREATED

QUEUED

PROCESSING

SENT_TO_SES

DELIVERED

BOUNCED

COMPLAINED

RETRY_WAITING

FAILED_FINAL

EXPIRED
```

---

# 7. Capacity

## Average

```
1,000,000/day

=

11.57 email/s
```

---

## Drain Time

12/s

```
100k

≈ 2h19m
```

500k

```
≈11h35m
```

1M

```
≈23h09m
```

---

14/s

```
≈19h50m
```

---

# 8. Why Queue?

Không dùng Queue

```
Kafka Consumer

↓

SES

↓

429

↓

Kafka Lag
```

Có Queue

```
Kafka

↓

DB

↓

SQS

↓

SES
```

Kafka luôn consume nhanh.

---

# 9. Monitoring

CloudWatch

Prometheus

Grafana

Theo dõi

- Queue Depth
- Oldest Message Age
- Send Rate
- Success Rate
- Bounce Rate
- Complaint Rate
- Retry Count
- DLQ Count

---

# 10. Alert

Ví dụ

Queue Age

>

30 phút

↓

Alert

---

DLQ

>

0

↓

Alert

---

SES Reputation

↓

Alert

---

Bounce Rate

>

5%

↓

Alert

---

Complaint Rate

>

0.1%

↓

Alert

---

# 11. Best Practices

✔ Transactional Outbox

✔ Idempotency

✔ Retry with Backoff

✔ DLQ

✔ Distributed Rate Limiter

✔ Long Polling

✔ Batch Processing

✔ Observability

✔ Event-driven Architecture

✔ Eventual Consistency

✔ Priority Queue

✔ Manual Redrive

✔ Suppression List

✔ Metrics & Alerting

---

# 12. Future Improvement

- Multi Region
- Multi SES Account
- Dynamic Rate Limiter
- Priority Scheduler
- Weighted Queue
- Email Personalization
- Template Versioning
- Bulk Send Optimization
- Campaign Scheduler
- Multi Provider (SES + SendGrid + Mailgun + SMTP)