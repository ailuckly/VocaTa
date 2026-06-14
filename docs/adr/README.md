# Architecture Decision Records

ADR records durable technical decisions that future contributors and AI coding agents
should not have to rediscover from commit history.

## When To Write One

Create an ADR when a change affects long-lived project behavior or maintenance cost:

- authentication, authorization, data writes, or API response contracts
- WebSocket streaming or the STT -> LLM -> TTS path
- third-party AI provider wiring
- database migration baseline or table-shape policy
- Docker, CI/CD, deployment, release, or rollback strategy
- key dependency additions, removals, or upgrades

Small local implementation details do not need an ADR.

## Naming

Use a zero-padded number and short kebab-case title:

```text
docs/adr/0001-use-flyway-for-database-migrations.md
```

Start from `0001`; keep `0000-template.md` as the reusable template.

## Status

Use one of these statuses:

- `Proposed`
- `Accepted`
- `Superseded`
- `Rejected`

When replacing a decision, keep the old ADR and link to the new one instead of deleting it.
