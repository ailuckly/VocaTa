# ADR 0001: Establish A Database Migration Baseline

Date: 2026-06-13

Status: Proposed

## Context

VocaTa currently has PostgreSQL entities and MyBatis Plus mappings, but no committed SQL
migration baseline. The current evidence is:

- `.ai-rules/database.md` states that the project has no SQL migration scripts.
- `vocata-server/src/main/resources/application-ci.yml` documents that CI uses an empty
  database and disables startup-time database side effects.
- `ContextLoadSmokeTest` verifies Spring context and Mapper parsing, but not business table
  availability.
- `README.md` and `docs/开发环境说明.md` warn that a new empty database may require a trusted
  backup or manual schema setup.

This blocks reproducible local setup, reliable CI business API smoke tests, onboarding, and
open-source contribution from a clean checkout.

Database changes are high risk in this repository. Introducing migrations would touch
`vocata_*` table shape, dependency/tooling policy, CI behavior, and rollout strategy, so the
implementation must happen in a dedicated PR after explicit approval.

## Decision

Introduce a database migration baseline in a dedicated follow-up PR, using Flyway unless a
later spike proves Liquibase is materially better for this project.

The first accepted baseline should:

- capture the current production/development table shape without inventing new schema
  semantics
- preserve existing MyBatis mappings, especially non-uniform audit fields such as
  `vocata_character.created_at` / `updated_at`
- keep `BIGINT` application-generated IDs, not `BIGSERIAL`
- avoid physical foreign keys unless a later ADR changes the database policy
- use `SMALLINT`, `JSONB`, PostgreSQL arrays, UUID columns, and timestamp types consistently
  with `.ai-rules/database.md`
- include a local empty-database validation path before enabling migration checks in CI

This ADR does not approve adding Flyway, creating SQL files, or changing tables in the
current documentation PR. It records the proposed direction and the acceptance criteria for
the follow-up work.

## Consequences

Benefits:

- A clean checkout can initialize a local database without private backups.
- CI can grow from context-only validation to schema-aware smoke tests.
- Future schema changes become reviewable and reversible through versioned files.
- AI agents and contributors get a concrete source of truth for table shape.

Trade-offs:

- The first baseline must be derived carefully from the trusted current schema, not only
  from entity classes.
- Existing environments need a baseline-on-existing-database plan instead of blindly
  reapplying `CREATE TABLE` statements.
- CI may become slower once schema validation and business smoke tests are enabled.
- Provider, WebSocket, and auth tests may need additional seed data or fake data fixtures.

Rollback path:

- Keep the first migration PR isolated.
- Validate on a throwaway database before touching shared development or staging databases.
- If migration activation breaks startup, revert the migration PR and return to the current
  manual schema setup while preserving this ADR for redesign.

## Alternatives Considered

- Option: Continue manual schema setup.
  - Why it was not chosen: it keeps local onboarding and CI business validation
    non-reproducible.
- Option: Commit raw `schema.sql` / `data.sql` without a migration tool.
  - Why it was not chosen: it helps first boot but does not create a durable versioned
    migration workflow.
- Option: Use Liquibase first.
  - Why it was not chosen: it is powerful for complex changelogs, but heavier than needed
    for the first baseline unless schema diff or multi-format changelogs become necessary.
- Option: Generate schema only from JPA/MyBatis entities.
  - Why it was not chosen: the project has non-uniform audit fields and PostgreSQL-specific
    types; generated schema could drift from the real database.

## Validation

The follow-up implementation PR should prove:

- `./scripts/validate-backend.sh` still passes.
- A new empty PostgreSQL database can be initialized from the committed baseline.
- The application context loads against the migrated empty database.
- At least one business API smoke path can run against baseline schema and seed/fake data.
- Existing trusted development/staging databases can be baselined without destructive DDL.
- The migration files contain no provider keys, tokens, production-only configuration, or
  sensitive seed data.

## Follow-Up Plan

Implementation details are tracked in `docs/数据库迁移基线方案.md`.
