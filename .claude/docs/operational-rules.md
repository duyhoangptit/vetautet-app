# Operational Rules

## 1. Operating mode

Current system mode:

```text
ANALYZE_AND_REPORT_ONLY
```

This is a hard product constraint.

## 2. Allowed actions

Claude may:
- inspect source code;
- inspect sanitized logs;
- inspect metrics;
- inspect traces;
- analyze incidents;
- generate code;
- generate tests;
- generate reports;
- update local audit state when explicitly implemented.

## 3. Forbidden actions

Claude must not:
- deploy;
- restart;
- scale;
- stop/start production processes;
- mutate production databases;
- execute production runbooks;
- change production configuration;
- modify firewall/network rules;
- rotate credentials;
- delete production data.

## 4. Human approval

Any future operational action must require explicit human approval.

An Ack is not approval for remediation.

Ack means:

```text
incident has been acknowledged
```

not:

```text
execute suggested action
```

## 5. Incident evidence

An analysis should contain:
- evidence;
- conclusion;
- uncertainty;
- recommended manual checks.

Do not fabricate evidence.

## 6. Context limits

Do not send an unlimited volume of logs to Claude.

Context fetchers should:
- bound time range;
- bound number of request IDs;
- bound number of log entries;
- prioritize relevant errors;
- truncate oversized fields;
- preserve timestamps and service names.

## 7. Duplicate alerts

Duplicate Alertmanager notifications must be deduplicated using a stable fingerprint/idempotency strategy.

## 8. Failure behavior

If Elasticsearch is unavailable:
- record context-fetch failure;
- do not invent context;
- do not ask Claude to analyze missing data as if it exists.

If Prometheus is unavailable:
- resource analysis should fail explicitly.

If Claude is unavailable:
- retain the alert/context for retry;
- do not silently drop it.

If Telegram is unavailable:
- retain the validated report in audit storage.

## 9. Claude response validation

The following pipeline is mandatory:

```text
raw response
 -> JSON parse
 -> Pydantic validation
 -> semantic validation
 -> persistence
 -> Telegram
```

No bypass.

## 10. Manual investigation

Recommendations must be safe, diagnostic steps such as:
- inspect dashboard;
- inspect connection pool;
- inspect slow query;
- inspect GC;
- inspect thread dump;
- inspect recent deployment;
- inspect downstream latency.

Do not generate direct commands that mutate infrastructure.

## 11. Future remediation

If remediation is introduced later, create a separate capability boundary.

Do not simply remove the current permission restrictions.

Required controls:
- action allow-list;
- approval;
- dry-run;
- audit;
- rollback;
- least privilege;
- rate limit;
- timeout;
- independent authorization.
