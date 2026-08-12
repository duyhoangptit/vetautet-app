---
name: testing
description: Build and execute tests for the AI Monitoring & Analysis Agent, including unit, integration, fixture, schema-validation, sanitizer, and end-to-end tests.
---

# Testing

## Test pyramid

```text
Unit
  |
Integration
  |
Contract
  |
End-to-end
```

Keep most tests at the unit level.

## Required fixtures

Maintain fixtures for:
- api_error alerts;
- resource alerts;
- Elasticsearch traces;
- Prometheus range responses;
- sanitized logs;
- Claude valid JSON;
- Claude malformed JSON;
- Claude schema-invalid JSON;
- Telegram failures.

## AI output tests

Test:
1. valid JSON;
2. malformed JSON;
3. missing fields;
4. invalid enum;
5. oversized summary;
6. empty lists where not allowed;
7. extra fields if strict mode is enabled.

## Sanitizer tests

Every newly supported sensitive pattern needs:
- positive test;
- negative test;
- edge case;
- regression test.

## External integrations

Use mocks/fakes for unit tests.

Integration tests may use containers or local services when available.

Never point automated tests at production.

## Failure scenarios

Explicitly test:
- Elasticsearch timeout;
- Prometheus timeout;
- Claude rate limit;
- Claude invalid response;
- Telegram timeout;
- database failure;
- duplicate alert.

## Completion

A feature is not complete until relevant tests pass.

For security-sensitive changes also run the security review checklist.
