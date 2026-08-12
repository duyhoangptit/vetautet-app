# Claude Code Workspace

This directory contains the project-specific Claude Code configuration for the AI Monitoring & Analysis Agent.

## Contents

- `settings.json` - project permissions and sandbox guardrails.
- `docs/` - architecture, security, coding, and operational rules.
- `skills/` - reusable procedures Claude can invoke automatically or with `/skill-name`.
- `agents/` - specialized project subagents.

## Important

This project is report-only.

The permission policy intentionally blocks common production-control commands such as:
- kubectl
- helm
- cloud CLIs
- ssh/scp
- Terraform apply/destroy

Review `.claude/settings.json` against the installed Claude Code version before committing it.

Run:

```text
/status
```

inside Claude Code to verify that the project settings are loaded.

Then:

```text
/doctor
```

to validate the setup.

Skills can be invoked explicitly:

```text
/code-generator
/api-analysis
/resource-analysis
/log-analysis
/security-sanitization
/prompt-engineering
/testing
```

Subagents can be requested by name:

```text
Use the architect agent to review this design.
Use the security-expert agent to review this change.
Use the code-reviewer agent before I commit.
```
