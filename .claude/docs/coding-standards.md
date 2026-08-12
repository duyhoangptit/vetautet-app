# Coding Standards

## 1. General

Use Python 3.12+ unless the repository explicitly requires another version.

Prefer:
- type hints;
- small functions;
- explicit dependencies;
- immutable value objects where practical;
- Pydantic for external data validation;
- dependency injection at application boundaries.

Avoid:
- global mutable state;
- hidden network calls;
- magic constants;
- broad `except Exception` without a clear recovery strategy.

## 2. Async

FastAPI endpoints should remain responsive.

Use async clients where the dependency supports them.

If a synchronous library must be used:
- isolate it;
- do not block the event loop accidentally;
- use a bounded executor when justified.

Do not create one thread per alert.

## 3. Domain

Domain models contain business concepts, not infrastructure concerns.

Examples:
- Alert
- AlertType
- Severity
- AnalysisResult
- InvestigationHint
- ReportStatus

Domain code must be deterministic and testable.

## 4. Application

Application services implement use cases.

Examples:
- AnalyzeApiError
- AnalyzeResourceAnomaly
- ProcessAlert
- AcknowledgeReport

Application services orchestrate ports but do not know concrete SDK classes.

## 5. Infrastructure

Infrastructure contains:
- Elasticsearch adapter;
- Prometheus adapter;
- Anthropic adapter;
- Telegram adapter;
- SQLite/PostgreSQL repositories.

Do not leak SDK response objects into the domain.

Map external responses into internal models.

## 6. Error handling

Classify failures.

Examples:

```text
ExternalTimeout
ExternalRateLimited
ExternalUnauthorized
ExternalBadRequest
ContextNotFound
SanitizationFailure
ClaudeInvalidJson
ClaudeSchemaViolation
TelegramDeliveryFailure
PersistenceFailure
```

Avoid using string matching on exception messages as the primary classification.

## 7. Retries

Retries must be:
- bounded;
- exponential/backoff;
- jittered where appropriate;
- limited to retryable failures.

Never retry indefinitely.

## 8. Timeouts

Every network request must have an explicit timeout.

No default infinite timeout.

Timeout values should be configuration-driven.

## 9. Configuration

Use typed settings.

Do not read environment variables directly throughout the application.

Prefer:

```text
config
   -> typed settings
   -> dependency construction
   -> application
```

## 10. Testing

Every new application use case should have unit tests.

External adapters should have integration/contract tests where practical.

Use fixtures for:
- API error alerts;
- resource alerts;
- Elasticsearch traces;
- Prometheus ranges;
- sanitized logs;
- Claude responses.

## 11. Claude output

Never trust a JSON response merely because the model was instructed to return JSON.

Always validate it with Pydantic.

Test:
- valid output;
- missing fields;
- invalid enum;
- malformed JSON;
- extra fields if strictness matters;
- empty investigation hints;
- excessively long summary.

## 12. Naming

Use explicit names.

Prefer:

```text
query_logs_by_request_id()
```

over:

```text
get_logs()
```

Prefer:

```text
AnalyzeApiErrorUseCase
```

over:

```text
Analyzer
```

## 13. Comments

Comments should explain why, not repeat what the code says.

## 14. Commits

Prefer small commits with one logical change.

Before commit:
1. run tests;
2. run lint;
3. inspect diff;
4. run security checks for security-sensitive changes.
