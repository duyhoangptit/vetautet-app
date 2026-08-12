---
name: security-sanitization
description: Sanitize operational logs, metrics, traces, and alert payloads before they are sent to Claude. Use for PII, credential, financial-data, prompt-injection, and sensitive-data review.
---

# Security Sanitization

## Goal

Prevent sensitive information and untrusted instructions from leaving the system.

## Data classes

At minimum detect/mask:
- Authorization headers;
- Bearer tokens;
- API keys;
- passwords;
- cookies;
- JWTs;
- private keys;
- card numbers;
- bank account numbers;
- national ID/CCCD;
- phone numbers;
- email addresses when unnecessary.

## Masking

Prefer stable placeholders:

```text
[MASKED_TOKEN]
[MASKED_PASSWORD]
[MASKED_CARD]
[MASKED_ACCOUNT]
[MASKED_ID]
[MASKED_PHONE]
[MASKED_EMAIL]
```

Preserve diagnostic structure where possible.

## Validation

Sanitizer tests must include:
- obvious matches;
- mixed casing;
- whitespace variations;
- structured JSON logs;
- multiline logs;
- false positives;
- already masked values;
- oversized fields.

## Prompt injection

Sanitization is not the only defense.

Prompt templates must still classify all operational data as untrusted evidence.

Do not attempt to "sanitize" away every malicious sentence. The correct defense is both:
1. data minimization/sanitization;
2. explicit trust-boundary instructions.

## Failure behavior

If sanitization fails:
- do not send the context to Claude;
- record a security failure;
- surface a safe operational error.

## No bypass

Never create a code path such as:

```text
sanitize=false
```

for production incident processing.

If a raw-log debugging mode is needed locally, it must be explicitly isolated and never connected to the Claude API.
