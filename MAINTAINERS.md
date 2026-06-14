# Maintainers

VocaTa is currently maintained as a personal open-source project. The repository owner is
the default maintainer for triage, review, release decisions, and security response unless
the project documents additional maintainers later.

## Responsibilities

Maintainers are responsible for:

- keeping `develop` reviewable and releasable
- triaging issues and pull requests
- protecting security-sensitive reports until a fix or mitigation is ready
- checking that AI-assisted changes include human review and real validation evidence
- keeping documentation, validation scripts, and CI aligned with the current project state
- recording out-of-scope technical debt instead of letting it disappear
- keeping AI review evidence and release notes tied to the actual validation that ran

## Review Ownership

Use these defaults when routing reviews:

| Area | Review focus |
| --- | --- |
| `vocata-server/` | API contracts, auth, data writes, WebSocket streaming, provider integrations |
| `vocata-web/` | user workflows, voice/text chat UX, API client contracts, responsive behavior |
| `vocata-admin/` | admin auth, operational workflows, data safety, validation |
| `docker-compose*.yml`, `.env.example`, deployment docs | environment parity, secrets, rollback risk |
| `.github/`, `scripts/` | CI behavior, local validation, contributor workflow |
| `docs/`, `.ai-rules/`, `AGENTS.md` | project rules, AI collaboration, development process |

## Triage Rules

- Security reports follow `SECURITY.md` and should not be discussed in public issues until safe.
- Bug reports should include reproduction steps and affected branch or commit.
- Feature requests should describe the user problem and the smallest useful solution.
- AI agent tasks should use `docs/AI任务模板.md` when scope, risks, or validation need to be explicit.
- AI agent reviews should use `docs/AI审查清单.md` so findings, assumptions, test gaps, and summaries stay consistent.
- Issue labels, priorities, close rules, and AI assignment guidance should follow `docs/维护与分流指南.md`.
- Changes touching schema, auth, WebSocket streaming, AI provider wiring, deployment, or dependencies need a clearly documented risk and rollback plan.

## Release Readiness

Before release or promotion, maintainers should confirm:

- relevant validation scripts passed
- PR risk and rollback notes are complete
- `CHANGELOG.md` includes user-visible changes
- `docs/发布检查清单.md` has been followed for release or promotion work
- `docs/版本发布策略.md` has been followed for release tags and release notes
- deployment docs match the target environment
- known remaining issues are either fixed or documented
