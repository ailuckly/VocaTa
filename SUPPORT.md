# Support

VocaTa is maintained as a personal open-source project. Support is best-effort, and
issue quality matters: a small reproducible report is much easier to act on than a broad
description.

## Where To Ask

- Bug reports: use the bug report issue template.
- Feature proposals: use the feature request issue template.
- Setup and workflow questions: use the question / support issue template.
- Security issues: follow `SECURITY.md` and do not open a public issue.
- Development workflow questions: start from `README.md`, `CONTRIBUTING.md`, and `docs/开发环境说明.md`.

## Before Opening An Issue

Please check:

- the current branch and commit
- whether `.env` was created from `.env.example`
- whether PostgreSQL and Redis are running
- whether the relevant validation script fails locally
- whether the issue reproduces on a clean browser session or fresh backend restart

Useful commands:

```bash
git status --short --branch
docker compose ps
./scripts/validate-backend.sh
./scripts/validate-web.sh
./scripts/validate-admin.sh
./scripts/validate-docker.sh
```

## What To Include

- expected behavior
- actual behavior
- reproduction steps
- logs with secrets redacted
- screenshots or screen recordings for UI issues
- module touched: backend, web, admin, Docker, CI, docs, or AI provider integration
