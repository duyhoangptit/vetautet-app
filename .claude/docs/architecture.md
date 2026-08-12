# Architecture

## 1. Purpose

This project is an AI Monitoring & Analysis Agent.

The system detects operational anomalies, gathers evidence, asks Claude to analyze the evidence, validates the structured result, and sends a report to Telegram.

The system is **Analyze & Report only**.

It must not automatically fix incidents, deploy software, restart services, scale infrastructure, mutate production data, or execute operational runbooks.

## 2. Main flow

```text
Prometheus / Alertmanager --------------------+
                                              |
ELK / Elasticsearch --------------------------+
                                              |
                                              v
                                      Alert Webhook
                                              |
                                              v
                                       Alert Router
                                        /         \
                                       /           \
                              api_error          resource
                                  |                  |
                                  v                  v
                         API Context Fetcher   Resource Context Fetcher
                                  |                  |
                                  +--------+---------+
                                           |
                                           v
                                      Sanitizer
                                           |
                                           v
                                     Prompt Builder
                                           |
                                           v
                                      Claude Client
                                           |
                                           v
                                  Pydantic Validation
                                           |
                                           v
                                   Analysis Result
                                           |
                              +------------+------------+
                              |                         |
                              v                         v
                       Report Formatter           Audit Repository
                              |
                              v
                        Telegram Bot
```

## 3. API error analysis

Trigger:
- Alertmanager alert with `alert_type=api_error`.
- Expected labels: `service`, `endpoint`, optional time range metadata.

Evidence:
1. Search ERROR logs in Elasticsearch.
2. Select approximately 3-5 representative request IDs.
3. Fetch the complete request trace for each request ID.
4. If tracing is available, optionally fetch Tempo/Jaeger spans.

Analysis:
- root cause service
- error category
- severity
- concise summary
- investigation hints

## 4. Resource analysis

Trigger:
- Prometheus/Alertmanager resource alert.
- Examples: CPU, memory, DB connection pool, thread count, GC, latency.

Evidence:
1. Query the triggering metric over approximately 15-30 minutes.
2. Query related metrics for correlation.
3. Fetch ERROR/WARN logs for the affected service and time range.

Analysis:
- likely cause
- transient vs degradation
- urgency
- manual checks

## 5. Dependency direction

```text
api
  -> application
      -> domain

infrastructure
  -> application/domain

security
  -> application/domain where necessary

prompts
  -> application
```

Domain code must not import:
- FastAPI
- Elasticsearch client
- Prometheus client
- Telegram SDK
- Anthropic SDK
- database drivers

Application code owns use cases and ports/interfaces.

Infrastructure implements those ports.

## 6. External boundaries

External systems:
- Alertmanager
- Prometheus
- Elasticsearch
- optional Tempo/Jaeger
- Anthropic API
- Telegram API
- SQLite/PostgreSQL

All external responses are untrusted input.

## 7. Structured output

Claude output is never treated as trusted text.

Required sequence:

```text
Claude response
   -> parse JSON
   -> Pydantic validation
   -> semantic validation
   -> persist audit record
   -> format Telegram report
```

If validation fails:
- do not send the result to Telegram as an incident report;
- record the failure;
- retry only according to bounded retry policy;
- if still invalid, produce an internal failure record.

## 8. Idempotency

The webhook path must tolerate duplicate alerts.

Use an alert fingerprint/idempotency key where available.

A duplicate alert must not cause unbounded duplicate Claude calls or Telegram messages.

## 9. Concurrency

Context fetching may be performed concurrently when calls are independent.

Do not introduce an unbounded worker pool.

External clients must have:
- timeouts;
- bounded concurrency;
- retry policy only for retryable failures;
- clear error classification.

## 10. Audit

At minimum persist:
- alert payload;
- normalized alert;
- fetched context snapshot;
- sanitizer result/metadata;
- Claude request metadata;
- Claude raw response if policy allows;
- validated analysis;
- Telegram report;
- processing timestamps;
- status/error.

Never persist secrets or unsanitized sensitive data unless explicitly required and protected.

## 11. Future extension

The architecture may later support approved remediation actions, but that is a separate capability.

Do not design today's report-only implementation around hidden assumptions that auto-fix will be enabled.

If remediation is introduced later, it must add:
- explicit action authorization;
- allow-listed runbooks;
- approval workflow;
- audit trail;
- dry-run;
- rollback;
- least-privilege credentials;
- independent policy enforcement.
