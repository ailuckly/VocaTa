# CLAUDE.md

Guidance for Claude Code (claude.ai/code) when working in this repository.

## Canonical source

`AGENTS.md` is the single source of truth for the agent working agreement: development
flow, decision boundaries (always / ask-first / never), definition of done, creativity
and autonomy, commit and PR rules, and validation commands. **Read `AGENTS.md` at the
start of a session and follow it.** If this file and `AGENTS.md` ever disagree,
`AGENTS.md` wins.

Detailed standards live in `.ai-rules/` (`backend.md`, `frontend.md`, `database.md`,
`tech.md`, `structure.md`) and `docs/`. Do not restate them here — link to them.

This file intentionally keeps only the Claude-specific essentials plus the
behavior-changing facts worth having in context immediately, because Claude Code loads
`CLAUDE.md` automatically but may not auto-load `AGENTS.md`.

## Project in one line

VocaTa is an AI role-playing platform for voice/text chat with characters. Voice path:
STT → LLM → TTS. Text chat: WebSocket streaming. Stack: Spring Boot 3.1.4 + Java 17 +
MyBatis Plus + Sa-Token + PostgreSQL + Redis; Vue 3 + Element Plus + Vite
(server `:9009`, web `:3000`, admin `:3001`).

## Critical coding rules (behavior-changing — keep in context)

These are the facts most likely to be guessed wrong. Everything else is in `AGENTS.md`
and `.ai-rules/`.

### No Lombok
Write getters/setters manually. Use static `fromEntity()` methods for entity→DTO mapping.

### ID fields: String in API, Long internally
DB `BIGINT` + snowflake (`@TableId(type = IdType.ASSIGN_ID)`); service layer uses `Long`;
API responses **always** serialize IDs as `String` (prevents JavaScript precision loss).

### Entities do NOT all extend BaseEntity
`User`, `Conversation`, `Message` do; `Character`, `UserFavorite`, `CharacterTag` do not.
Inspect the concrete class first and follow its existing mapping pattern. Do not force a
`BaseEntity` refactor unless that is the explicit task.

### Unified API response
All controllers return `ApiResponse<T>`; paginated results use `PageResult<T>`. Throw
`BizException(ApiCode.XXX)` for business errors — `GlobalExceptionHandler` formats them.
Do not leak raw stack traces or third-party responses to the frontend.

### Route auth (Sa-Token)
`/api/open/**` public · `/api/client/**` user · `/api/admin/**` admin. The `/api` prefix
is preconfigured, so `@RequestMapping("/client/user")` → `/api/client/user`. User context
via `UserContext.getUserId()` / `UserContext.checkAdmin()`.

### Database conventions
Table prefix `vocata_`, association tables end with `_relation`, no ENUM types (use
`SMALLINT`), no physical foreign keys, `JSONB` for JSON, `TIMESTAMP WITH TIME ZONE`,
soft delete via `@TableLogic` on `isDelete` (0=active, 1=deleted).

### Validation DTOs
Request DTOs use `@Valid` with JSR-303 annotations (`@NotBlank`, `@Email`, `@Length`, …).

## Commands

```bash
# Backend (port 9009)
set -a && source .env && set +a                # export local env first
cd vocata-server && mvn spring-boot:run        # default profile (local)
mvn -Dmaven.repo.local=/tmp/juhao_m2repo spring-boot:run
mvn clean package -DskipTests                  # build JAR
mvn test                                        # run tests

# Web (:3000) / Admin (:3001)
cd vocata-web && npm install && npm run dev
cd vocata-admin && npm install && npm run dev

# Full Docker mode for container-build validation
docker compose up -d --build

# Local quality gate (docs/metadata + backend + web + admin)
./scripts/check.sh
```

Daily development normally uses Docker only for PostgreSQL / Redis:
`docker compose up -d postgres redis`. Full command list, env profiles, CI/CD details,
and the module map: see `AGENTS.md`, `docs/开发环境说明.md`, and `.ai-rules/`.
