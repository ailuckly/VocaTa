# AGENTS.md

This file provides repository-specific guidance for coding agents working in this project.

## Project Overview

VocaTa is an AI role-playing platform for voice and text conversations with characters. The main runtime path for voice chat is STT -> LLM -> TTS, and text chat is handled through WebSocket streaming.

Tech stack:
- Backend: Spring Boot 3.1.4, Java 17, MyBatis Plus 3.5.3.2, Sa-Token 1.37.0, PostgreSQL, Redis
- Frontend: Vue 3, Vite, Element Plus, Pinia
- Admin: Vue 3, Vite, Element Plus

## Repository Layout

- `vocata-server/` - backend service, runs on port `9009`
- `vocata-web/` - user-facing frontend, runs on port `3000`
- `vocata-admin/` - admin frontend, runs on port `3001`
- `scripts/` - local and CI validation scripts
- `.ai-rules/` - internal project conventions and architecture notes
- `docs/` - workflow, deployment, and validation documentation

## Recommended Commands

### Backend

```bash
cd vocata-server
mvn spring-boot:run
mvn spring-boot:run -Dspring-boot.run.profiles=local
mvn clean package -DskipTests
mvn test
```

Notes:
- `application.yml` currently sets `spring.profiles.active=local` by default.
- Local backend validation is standardized through `scripts/validate-backend.sh`.
- In constrained environments, this repository often uses `-Dmaven.repo.local=/tmp/juhao_m2repo`.

### Frontend Web

```bash
cd vocata-web
npm install
npm run dev
npm run type-check
npm run test
npm run build
```

### Frontend Admin

```bash
cd vocata-admin
npm install
npm run dev
npm run type-check
npm run build
```

### Validation Scripts

Prefer the repository scripts when validating work:

```bash
./scripts/check.sh
./scripts/validate-backend.sh
./scripts/validate-web.sh
./scripts/validate-admin.sh
./scripts/validate-docker.sh
```

Use `./scripts/check.sh` for a full local quality gate. Use the module-specific scripts when a narrow change only touches one area.

Current script behavior:
- `validate-backend.sh` packages the backend and runs tests
- `validate-web.sh` runs `eslint`, `type-check`, `test`, and `build` in `vocata-web`
- `validate-admin.sh` runs `eslint`, `type-check`, and `build` in `vocata-admin`

## Architecture

The project mostly follows:

```text
Controller -> Service -> Mapper -> PostgreSQL / Redis / external AI providers
```

Typical backend module layout:

```text
com.vocata.{module}/
├── controller/
├── service/
│   └── impl/
├── mapper/
├── entity/
├── dto/
└── constants/
```

Implemented areas include:
- `auth`
- `user`
- `character`
- `conversation`
- `ai`
- `voice`
- `file`
- `admin`
- `common`
- `config`

## AI / Streaming Notes

The real-time conversation path is centered on `AiChatWebSocketHandler`.

Key abstractions:
- `LlmProvider` with multiple implementations such as Qiniu, OpenAI, Gemini, and SiliconFlow
- `SttClient` for speech-to-text providers
- `TtsClient` for text-to-speech providers
- `AiStreamingService` for streamed model output

Third-party HTTP calls use Spring `WebClient`.

## Authentication and Routing

Sa-Token route split:
- `/api/open/**` - public
- `/api/client/**` - authenticated user APIs
- `/api/admin/**` - admin APIs

Useful backend helpers:
- `UserContext.getUserId()`
- `UserContext.isAdmin()`
- `UserContext.checkAdmin()`

Token style:
- `Authorization: Bearer <token>`

## API Conventions

Controllers return `ApiResponse<T>`. Paginated responses use `PageResult<T>`.

General URL convention:
- The application is mounted under `/api`
- Controller `@RequestMapping("/client/user")` becomes `/api/client/user`

ID handling:
- Database and service layer use `Long`
- API responses often serialize IDs as `String` to avoid JavaScript precision loss

Exception handling:
- Business errors should throw `BizException`
- `GlobalExceptionHandler` converts exceptions to unified API responses

## Entity and Persistence Rules

Do not assume every entity extends `BaseEntity`.

Current codebase reality:
- Many entities do extend `BaseEntity`, for example `User`, `Conversation`, and `Message`
- Some important entities do not, for example `Character`, `UserFavorite`, and `CharacterTag`

So when editing an entity:
- inspect the concrete class first
- follow the existing field mapping pattern in that file
- do not force a `BaseEntity` refactor unless that is the task

Other persistence notes:
- Table names generally use the `vocata_` prefix
- Soft delete is used in parts of the codebase through `@TableLogic`
- PostgreSQL arrays and JSON/JSONB are already in use in the character/tag area

## Coding Rules

- Do not introduce Lombok into this repository
- Follow the existing manual getter/setter style in backend DTOs and entities
- Prefer matching the surrounding code over imposing a new pattern
- Validate request DTOs with JSR-303 annotations where that pattern is already used

Before making assumptions about a rule, check:
- `.ai-rules/backend.md`
- `.ai-rules/frontend.md`
- `.ai-rules/database.md`
- `.ai-rules/tech.md`
- `.ai-rules/structure.md`

## AI Agent Working Agreement

### Project Goal

VocaTa should stay maintainable for long-running personal development and AI-assisted collaboration. Changes should improve the product without making the architecture harder to reason about, test, deploy, or review.

### Creativity and Autonomy

Be ambitious about *how* to solve a problem; be strict about *house style, fragile areas, and safety*. These are fixed precisely so that solution design can stay free.

- On solution design — algorithms, data flow, component structure, refactors within one module — propose and pursue the best approach. Suggesting a better design than the one requested is welcome.
- On house style, project conventions, fragile core paths, and safety, follow the rules exactly. This is the non-negotiable frame, not a place to improvise.
- When a rule blocks a clearly better solution, say so and propose the alternative. Do not silently work around it, and do not silently abandon the better idea.

### Development Flow

1. Start from `develop` or the existing task branch.
2. Inspect the relevant module before proposing or editing code.
3. Keep each change focused on one problem.
4. Prefer small, verifiable increments over broad rewrites.
5. Run the validation command that matches the touched module.
6. Summarize what changed, what was verified, and any remaining risk.

### Before Editing Code

Before modifying files, check:

- Current branch and worktree status with `git status --short --branch`.
- Existing docs that apply to the change: `README.md`, `CONTRIBUTING.md`, `CODE_STYLE.md`, `.ai-rules/*`, and this file.
- Nearby implementation patterns in the same module.
- Existing tests or validation scripts that should cover the change.
- Whether the requested change risks touching auth, data writes, API contracts, WebSocket streaming, third-party AI providers, or deployment configuration.

If existing user changes are present, do not revert or overwrite them unless explicitly asked.

### Code Style Requirements

- Follow `CODE_STYLE.md`.
- Backend code uses Java 17, Spring Boot conventions, explicit getters/setters, and no Lombok.
- Frontend code uses Vue 3 Composition API, TypeScript, Pinia, Vue Router, SCSS, ESLint, and Prettier.
- Keep API response IDs as strings where the frontend may consume large numeric IDs.
- Match existing file style before introducing a new abstraction.

### Testing Requirements

Run checks by touched area:

```bash
./scripts/validate-backend.sh
./scripts/validate-web.sh
./scripts/validate-admin.sh
./scripts/validate-docker.sh
```

For targeted frontend work, also consider:

```bash
cd vocata-web && npm run test
```

If a command cannot be run or fails for an environmental reason, report the exact command, the failure, and the remaining risk.

### Definition of Done

A change is "done" only when all of the following hold. Do not claim a change passes, works, or is complete without the evidence below.

- The validation script for every touched area was actually run, and its real output is reported. Never state "tests pass" or "build succeeds" from assumption — run it and show it.
- New behavior or a bug fix has a test that would fail without the change, where the area is testable.
- The change stays within the scope that was agreed; unrelated fixes found along the way are reported, not silently bundled in.
- Known issues that are intentionally left for later are recorded in the backlog (see below), not dropped.
- A short summary states what changed, what was verified (with the commands run), and any remaining risk.

If any item cannot be satisfied, say so explicitly instead of implying completion.

### Known Issues and Tech Debt

When you find a real problem that is out of scope for the current task, do not silently fix it and do not silently ignore it. Record it so it survives across sessions and agents. Until a dedicated backlog file exists, list it in the task summary and propose where it should live. Do not let discovered debt disappear.

### Git Commit Requirements

Use:

```text
<type>: <summary>
```

Allowed types:

- `feat`
- `fix`
- `refactor`
- `docs`
- `test`
- `chore`
- `style`
- `perf`
- `build`
- `ci`
- `revert`

Commit rules:

- One commit should do one thing.
- Use an English verb phrase for the summary.
- Do not commit unrelated changes.
- Do not commit temporary debug code.
- Split large changes into multiple reviewable commits.

### PR Requirements

PRs should target `develop` unless the release process requires otherwise.

Before merge:

- lint passes
- typecheck passes where applicable
- tests pass or missing coverage is documented
- build passes
- PR description is complete
- risk and rollback notes are included
- screenshots or demo notes are included for UI changes

PR descriptions should use Chinese by default. Use English only when a specific external collaboration or upstream convention requires it.

### Git Hook Tooling Decision

Do not introduce `commitlint`, `husky`, or `lint-staged` for now.

Trade-off:

- They can catch bad commit messages and formatting issues earlier.
- They add dependencies and hook maintenance overhead.
- They can complicate temporary fixes, cross-platform setup, and AI agent execution.
- Current project safeguards already include docs, PR checklist, CI, and `./scripts/check.sh`.

Revisit this only if collaboration grows or commit/format drift becomes frequent.

### Decision Boundaries

Three tiers. When an action is not listed, judge it by the nearest listed example; if it sits close to the "ask first" line, ask.

**Always (act autonomously, no need to ask):**

- Read any code, run any validation script, run tests, inspect git status.
- Edit code within a single module to implement the requested change.
- Choose the implementation approach, naming, and internal structure for that change.
- Add or update tests covering your change.
- Fix an obvious local bug or typo discovered inside the file you are already editing.

**Ask first (stop and confirm before doing):**

- Changing database schema, migrations, or any `vocata_*` table shape.
- Deleting files, or moving/renaming across modules.
- Adding, removing, or upgrading a dependency (backend or frontend).
- Touching auth / Sa-Token route rules, `UserContext`, or permission checks.
- Changing fragile core paths: `AiChatWebSocketHandler`, the STT→LLM→TTS streaming chain, provider wiring.
- Changing deployment, Docker, CI, or environment configuration.
- Any change whose blast radius crosses more than one module.

**Never (do not do, even if it seems convenient):**

- Do not perform large architecture rewrites without a written plan and explicit approval.
- Do not move many files just to make the tree look cleaner.
- Do not introduce heavy dependencies without explaining need, alternatives, and complexity impact.
- Do not commit secrets, tokens, passwords, private keys, or real production-only config.
- Do not weaken auth, validation, error handling, or logging safeguards for convenience.
- Do not replace existing tools such as Maven, Vite, ESLint, Prettier, or Vitest unless that is the explicit task.
- Do not hide formatting-only changes, build-config changes, or unrelated fixes inside a feature or fix commit.

## Frontend Notes

Both frontends use:
- Vue 3 Composition API with `<script setup>`
- Pinia
- Vue Router 4
- Axios-based API modules
- SCSS
- ESLint and Prettier

`vocata-web` also has:
- Vitest test setup
- `npm run test`

## CI / CD

Current GitHub workflows:
- `ci.yml`
- `cd-staging.yml`
- `cd-production.yml`
- `release.yml`
- `emergency-rollback.yml`

Current CI facts:
- `ci.yml` runs on pull requests and pushes to `develop` and `master`
- it uses path filtering so backend, web, and admin checks only run when those areas change
- changes to `scripts/**` or `.github/workflows/ci.yml` trigger all three validation areas
- backend validation uses `scripts/validate-backend.sh`
- frontend validation uses `scripts/validate-web.sh` and `scripts/validate-admin.sh`

## Branching and Collaboration

Current remote branches visible in this repository:
- `origin/develop`
- `origin/master`

Practical workflow:
- branch from `develop` for feature or fix work
- open PRs into `develop`
- promote from `develop` toward release flow as needed

If a worktree already exists for a feature branch, prefer continuing in that worktree instead of switching the main workspace back and forth.

## Documentation References

Useful project docs:
- `docs/开发工作流.md`
- `docs/开发环境说明.md`
- `docs/部署环境说明.md`
- `docs/验证清单.md`
- `docs/提交规范.md`
