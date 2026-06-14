# ADR 0002: Add Dependency And Security Automation

Date: 2026-06-13

Status: Proposed

## Context

VocaTa already has a useful CI baseline:

- PR title and commit message checks in `.github/workflows/ci.yml`.
- Backend, web, and admin validation jobs with path filtering.
- PR-level gitleaks scanning in `.github/workflows/ci.yml`.
- Production and staging deployment workflows.

The repository does not yet have:

- Dependabot configuration for Maven, npm, Docker/GitHub Actions ecosystems.
- CodeQL static analysis for Java and JavaScript/TypeScript.
- A versioned `.gitleaks.toml` for custom rules, allowlists, and ignore policy.
- CODEOWNERS for automatic review routing.

Adding these tools changes GitHub automation behavior. It can create PR noise, increase CI
runtime, introduce false positives, and require ongoing maintenance. Therefore the first
step should be an explicit plan and a dedicated follow-up PR, not a silent config drop in a
documentation branch.

## Decision

Adopt dependency and security automation in small, reviewable increments:

1. Add Dependabot first for GitHub Actions, Maven, `vocata-web`, and `vocata-admin`.
2. Add CodeQL after Dependabot noise is understood.
3. Add a `.gitleaks.toml` only after collecting real false positives or project-specific
   rules that justify versioning the configuration.
4. Add CODEOWNERS only after the maintainer/review ownership boundaries are stable.

This ADR does not approve adding those config files in the current PR. It records the
recommended order, risk controls, and acceptance criteria for future PRs.

## Consequences

Benefits:

- Dependency and action updates become visible instead of relying on manual audits.
- Security scanning moves closer to standard open-source expectations.
- Future contributors and AI agents get clearer signals about stale dependencies and
  vulnerable patterns.
- Review routing can become more predictable once CODEOWNERS is safe to enable.

Trade-offs:

- Dependabot may open noisy PRs, especially for npm ecosystems.
- CodeQL may add runtime and false positives.
- A gitleaks config can accidentally weaken scanning if allowlists are too broad.
- CODEOWNERS can create review friction if ownership boundaries are too vague.

Rollback path:

- Keep each automation in a separate PR.
- Disable or narrow one automation at a time if it creates unacceptable noise.
- Revert the individual config PR rather than touching unrelated workflows.
- Preserve this ADR and update it with lessons learned.

## Alternatives Considered

- Option: Keep manual dependency/security checks.
  - Why it was not chosen: manual checks are easy to forget and do not meet mature
    open-source expectations.
- Option: Add Dependabot, CodeQL, gitleaks config, and CODEOWNERS in one PR.
  - Why it was not chosen: too much automation changes at once, hard to debug noise and
    false positives.
- Option: Add CODEOWNERS immediately.
  - Why it was not chosen: this is currently a personal project, and review ownership
    should stabilize first.
- Option: Replace the current gitleaks install step with another secret scanner.
  - Why it was not chosen: current PR-level gitleaks scanning already exists and should be
    hardened before being replaced.

## Validation

Future implementation PRs should prove:

- New automation config parses correctly.
- It does not require secrets beyond the default GitHub token unless explicitly documented.
- It does not weaken existing CI, validation scripts, or secret scanning.
- Generated PR volume and schedule are acceptable for a personal open-source project.
- False-positive handling is documented without allowing broad secret bypasses.

## Follow-Up Plan

Implementation details are tracked in `docs/依赖与安全自动化方案.md`.
