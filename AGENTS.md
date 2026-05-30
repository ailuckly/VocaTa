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
./scripts/validate-backend.sh
./scripts/validate-web.sh
./scripts/validate-admin.sh
./scripts/validate-docker.sh
```

Current script behavior:
- `validate-backend.sh` packages the backend and runs tests
- `validate-web.sh` runs `eslint`, `type-check`, and `build` in `vocata-web`
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
- `ci.yml` runs on pull requests to `develop` and `master`
- it uses path filtering so backend, web, and admin checks only run when those areas change
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
