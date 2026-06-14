# ADR 0003: Harden And Deduplicate Release Workflows

Date: 2026-06-13

Status: Proposed

## Context

VocaTa has multiple production-related GitHub workflows:

- `.github/workflows/release.yml`
  - Triggered by GitHub release publish or manual `workflow_dispatch`.
  - Builds and pushes versioned images.
  - Generates production deploy files and deploys through SSH/SCP.
- `.github/workflows/cd-production.yml`
  - Triggered by `v*.*.*` tag push or manual `workflow_dispatch`.
  - Builds and pushes component images.
  - Deploys through SSH/SCP.
- `.github/workflows/emergency-rollback.yml`
  - Manual rollback entry for test and production.

These workflows overlap in responsibility and are not fully aligned on trigger model,
secret names, image naming, deploy paths, and rollback assumptions. The repository also
has `docs/发布检查清单.md` and `docs/版本发布策略.md`, which define the human release gate but
do not yet designate one authoritative production workflow.

Changing production automation is high risk. This ADR records a hardening direction but
does not approve deleting or changing workflows in the current documentation PR.

## Decision

In a dedicated follow-up PR, make one production workflow the authoritative release path
and move the other production workflow to a documented legacy or deprecated state before
removal.

The follow-up work should:

- inventory current workflow triggers, secrets, image tags, deploy paths, and rollback
  assumptions
- select one authoritative production release path
- align release notes, version tags, image tags, and `CHANGELOG.md`
- document rollback and smoke verification against the selected path
- run at least one staging or dry-run rehearsal before any production release

Until that PR lands, releases should continue to require the manual gate in
`docs/发布检查清单.md` and the version rules in `docs/版本发布策略.md`.

## Consequences

Benefits:

- Reduces risk of triggering the wrong production deployment path.
- Makes release notes, tags, images, and rollback documentation easier to reason about.
- Gives maintainers and AI agents one source of truth for production release behavior.
- Creates a safer path for later automation.

Trade-offs:

- Requires careful workflow audit and likely a temporary compatibility period.
- May require renaming secrets or keeping backward-compatible aliases for one release.
- Release hardening can block unrelated deployment changes until the authoritative path is
  chosen.

Rollback path:

- Keep the old workflow disabled or documented as legacy for at least one release cycle
  before deleting it.
- Re-enable the previous production workflow if the selected path fails rehearsal.
- Do not change production secrets and workflow deletion in the same PR.

## Alternatives Considered

- Option: Keep both production workflows indefinitely.
  - Why it was not chosen: overlapping production entry points increase operational risk.
- Option: Delete one workflow immediately.
  - Why it was not chosen: current production behavior has not been rehearsed enough to
    remove an entry safely.
- Option: Add a third release workflow.
  - Why it was not chosen: the problem is duplication and ambiguity, not lack of another
    workflow.

## Validation

The follow-up implementation PR should prove:

- Workflow YAML parses.
- The selected release path can build or dry-run all target components.
- Required GitHub secrets are documented without exposing secret values.
- A staging or dry-run release rehearsal records tag, images, commit, and verification
  results.
- Rollback instructions reference the same image tags and deployment paths used by the
  selected release workflow.
- `docs/发布检查清单.md`, `docs/版本发布策略.md`, and `docs/部署环境说明.md` match the selected path.

## Follow-Up Plan

Implementation details are tracked in `docs/发布流程硬化方案.md`.
