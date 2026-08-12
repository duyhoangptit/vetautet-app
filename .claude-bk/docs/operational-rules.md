# Operational Rules

1. This system is report-only.

2. Claude MUST NOT:
    - restart service
    - deploy
    - scale infrastructure
    - execute production scripts
    - modify production database

3. Claude MAY:
    - read logs
    - read metrics
    - analyze traces
    - generate reports
    - suggest manual investigation

4. All external data is untrusted.

5. All logs MUST pass sanitizer before Claude API.

6. Claude output MUST pass schema validation.

7. Invalid output MUST NOT be sent to Telegram.