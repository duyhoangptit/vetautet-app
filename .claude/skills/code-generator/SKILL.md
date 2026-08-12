---
name: code-generator
description: Generate production-quality Python code for the AI Monitoring & Analysis Agent following its architecture, dependency direction, typing, testing, and security rules. Use when implementing new features, adapters, use cases, models, API endpoints, or tests.
---

# Code Generator

## Mission

Implement code that fits the existing architecture instead of inventing parallel patterns.

Read before changing code:
- `.claude/docs/architecture.md`
- `.claude/docs/coding-standards.md`
- `.claude/docs/security.md`
- `.claude/docs/operational-rules.md`

## Workflow

1. Inspect the existing repository structure.
2. Identify the closest existing implementation.
3. Determine whether the change belongs to:
   - API;
   - application;
   - domain;
   - infrastructure;
   - security;
   - prompts;
   - tests.
4. Preserve dependency direction.
5. Define or reuse a port/interface before creating an infrastructure adapter.
6. Implement the smallest coherent change.
7. Add tests.
8. Run relevant tests and lint/type checks.
9. Review the final diff.
10. Report assumptions and remaining risks.

## Architecture rules

Do not:
- put business logic inside FastAPI route handlers;
- import SDK clients into domain models;
- call Elasticsearch/Prometheus/Anthropic directly from domain code;
- create generic `utils.py` files for unrelated functionality;
- duplicate configuration parsing.

## External adapters

Adapters must:
- receive typed configuration;
- have explicit timeouts;
- classify errors;
- return internal models;
- avoid leaking SDK-specific response objects.

## Security

Never:
- read secrets to satisfy an implementation request;
- place credentials in source code;
- bypass the sanitizer;
- send raw logs to Claude;
- add production execution capability.

## Tests

For each new behavior provide:
- happy path;
- invalid input;
- external failure;
- timeout/retry behavior where applicable.

## Completion criteria

Do not claim completion until:
- code compiles/imports;
- relevant tests pass;
- lint/type checks pass when configured;
- no secret was introduced;
- architecture boundaries remain intact.
