# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

VocaTa is an AI role-playing platform where users have voice and text conversations with characters (Harry Potter, Socrates, etc.). The core pipeline is STT → LLM → TTS for voice chat, with text chat also supported via WebSocket streaming.

**Tech Stack**: Spring Boot 3.1.4 + Java 17 + MyBatis Plus 3.5.3.2 + Sa-Token 1.37.0 + PostgreSQL + Redis (Lettuce/Redisson) + Vue 3 + Element Plus + Vite

## Build & Run Commands

### Backend (vocata-server, port 9009)
```bash
cd vocata-server
mvn spring-boot:run                                    # default profile
mvn spring-boot:run -Dspring-boot.run.profiles=local   # local profile (cloud DB)
mvn clean package -DskipTests                          # build JAR
mvn test                                               # run tests
```

### Frontend Client (vocata-web, port 3000)
```bash
cd vocata-web && npm install && npm run dev
npm run build
```

### Admin Dashboard (vocata-admin, port 3001)
```bash
cd vocata-admin && npm install && npm run dev
npm run build:test
```

### Docker
```bash
docker-compose up -d                    # all services (postgres, redis, backend, frontend, admin, pgAdmin, MailHog)
docker-compose up -d postgres redis     # just databases
docker-compose logs -f vocata-server    # backend logs
docker-compose exec postgres psql -U vocata -d vocata_local
```

### Validation Scripts
```bash
scripts/validate-backend.sh    # backend validation
scripts/validate-web.sh        # frontend web validation
scripts/validate-admin.sh      # admin frontend validation
```

## Architecture

### Layered Architecture (strict)
```
Controller → Service → Mapper → PostgreSQL/Redis
```
Each business module follows this structure:
```
com.vocata.{module}/
├── controller/     # REST endpoints
├── service/        # Business logic interfaces
│   └── impl/       # Implementations
├── mapper/         # MyBatis Plus data access
├── entity/         # Database entities (extend BaseEntity)
├── dto/            # Request/Response DTOs
└── constants/      # Module constants
```

### Implemented Modules
| Module | Package | Description |
|--------|---------|-------------|
| auth | `com.vocata.auth` | Registration (email verification), login, logout, password reset |
| user | `com.vocata.user` | User info, profile, favorites (`UserFavoriteService`) |
| character | `com.vocata.character` | Character CRUD, tags, chat counts, AI character generation |
| conversation | `com.vocata.conversation` | Conversations, messages, auto title generation |
| ai | `com.vocata.ai` | Multi-LLM streaming, STT/TTS clients, WebSocket chat handler |
| voice | `com.vocata.voice` | TTS voice management, voice profiles, voice resolver |
| file | `com.vocata.file` | Qiniu Cloud file upload |
| admin | `com.vocata.admin` | Admin auth, user management, TTS voice admin |
| common | `com.vocata.common` | BaseEntity, ApiResponse, exceptions, utilities |
| config | `com.vocata.config` | SaTokenConfig, MybatisPlusConfig, RedisConfig, WebSocketConfig, WebClientConfig, WebMvcConfig |

### AI Streaming Architecture
Real-time AI chat uses WebSocket (`AiChatWebSocketHandler`):
- **LLM Provider abstraction**: `LlmProvider` interface with implementations: `QiniuLlmProvider`, `OpenAiLlmProvider`, `GeminiLlmProvider`, `SiliconFlowLlmProvider`
- **STT**: `SttClient` interface → `QiniuSttClient`, `XunfeiWebSocketSttClient`
- **TTS**: `TtsClient` interface → `VolcanTtsClient`, `XunfeiStreamTtsClient`
- **Streaming**: `AiStreamingService` handles streaming responses; `AiPromptEnhanceService` for prompt optimization
- **Non-blocking HTTP**: Spring WebFlux `WebClient` for third-party API calls (configured in `WebClientConfig`)

### Authentication & Route Protection (Sa-Token)
- `/api/open/**` — Public, no auth
- `/api/client/**` — Client APIs, user auth required
- `/api/admin/**` — Admin only
- User context: `UserContext.getUserId()` / `UserContext.checkAdmin()`
- Token: `Authorization: Bearer <token>`

### Unified API Response
All controllers return `ApiResponse<T>`. Paginated results use `PageResult<T>`.
```json
{"code": 200, "message": "成功", "data": {}, "timestamp": 1650000000000}
```

### Exception Handling
- Throw `BizException(ApiCode.XXX)` for business errors
- `GlobalExceptionHandler` converts all exceptions to `ApiResponse` format
- Error codes defined in `ApiCode` enum

## Critical Coding Rules

### No Lombok
The project does NOT use Lombok. Write getters/setters manually. Use static `fromEntity()` methods for entity→DTO mapping:
```java
public static UserInfoResponse fromEntity(User user) {
    UserInfoResponse response = new UserInfoResponse();
    response.setId(String.valueOf(user.getId()));
    // ...
    return response;
}
```

### ID Fields: String in API, Long internally
- Database: `BIGINT` + snowflake ID (`@TableId(type = IdType.ASSIGN_ID)`)
- Service layer: `Long`
- API responses: **Always `String`** (prevents JavaScript precision loss)

### Entity Requirements
- All entities extend `BaseEntity` (auto-fills: `createId`, `createDate`, `updateId`, `updateDate`, `isDelete`)
- Use `@TableName("vocata_xxx")` for table mapping
- Soft delete via `@TableLogic` on `isDelete` (0=active, 1=deleted)

### Database Design
- Table prefix: `vocata_`, association tables end with `_relation`
- No ENUM types — use SMALLINT for enum values
- No physical foreign keys — use association tables
- JSON: use JSONB type
- Timestamps: `TIMESTAMP WITH TIME ZONE`
- Audit fields on every table: `create_id`, `update_id`, `create_date`, `update_date`, `is_delete`

### API URL Convention
The `/api` prefix is preconfigured. Controller mappings use the path after `/api`:
```java
@RestController
@RequestMapping("/client/user")   // actual URL: /api/client/user
```

### Parameter Validation
All request DTOs use `@Valid` with JSR-303 annotations (`@NotBlank`, `@Email`, `@Length`, etc.).

## Environment Configuration

Profiles: `local` (default dev, cloud DB), `test`, `prod`. Switch via `--spring.profiles.active=xxx`.

| | Local | Test | Prod |
|---|---|---|---|
| Config | `application.yml` + `application-local.yml` | `application-test.yml` | `application-prod.yml` |
| DB | Cloud PostgreSQL (Aiven) | Test-dedicated DB | Production DB |
| Redis DB | 0 | 1 | 0 |
| Log level | DEBUG | INFO | INFO |
| Log file | `logs/vocata-local.log` | `logs/vocata-test.log` | `/var/log/vocata/vocata-server.log` |

## CI/CD (GitHub Actions)

Workflows in `.github/workflows/`:
- **ci.yml** — Triggers on PR to `develop`/`master` and push to `develop`. Runs backend build + tests, frontend type-check + lint + build for both web and admin.
- **cd-staging.yml** — Deploy to staging
- **cd-production.yml** — Deploy to production
- **release.yml** — Release management
- **emergency-rollback.yml** — Emergency rollback

## Git Workflow

1. Branch from `develop`: `feat/描述`, `fix/描述`
2. PR to `develop` → CI runs automatically
3. Merge to `develop` → staging deploy
4. `develop` → `master` for releases

## Frontend Architecture (Vue 3)

Both `vocata-web` and `vocata-admin` use:
- **Vue 3 Composition API** (`<script setup>`)
- **Element Plus** UI components
- **Pinia** for state management
- **Vue Router 4** with lazy loading
- **Axios** for HTTP (with interceptors)
- **SCSS** with BEM naming
- **ESLint + Prettier** for code quality

## Reference Documentation

Detailed development standards are in `.ai-rules/`:
- `backend.md` — Full backend coding standards, naming conventions, layer rules
- `frontend.md` — Vue 3 component design, SCSS organization, state management patterns
- `database.md` — Complete PostgreSQL schema design rules
- `tech.md` — Technical architecture overview
- `product.md` — Product vision and feature planning
- `structure.md` — Project structure standards

Additional docs in `docs/`:
- `开发工作流.md`, `开发环境说明.md`, `部署环境说明.md`, `验证清单.md`, `提交规范.md`
