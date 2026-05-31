---
title: Project Structure
description: "定义 VocaTa 的目录结构、模块组织和命名约定。"
inclusion: always
---

# VocaTa 项目结构规范

> 本文件描述项目的**真实目录结构与命名约定**。跨文件通用规则（决策边界、提交规范、Definition of Done）见 `AGENTS.md`；编码细则见 `.ai-rules/backend.md`、`.ai-rules/frontend.md`、`.ai-rules/database.md`。本文件只在此处权威化「结构与命名」，避免与上层文档重复。

## 仓库根目录

```
VocaTa/
├── .ai-rules/             # AI 协作技术细则（backend/frontend/database/tech/product/structure）
├── AGENTS.md              # 唯一权威的 AI 工作契约（决策边界 / DoD / 提交规范）
├── CLAUDE.md              # Claude Code 入口，指向 AGENTS.md
├── CONTRIBUTING.md        # 贡献指南
├── CODE_STYLE.md          # 代码风格
├── README.md              # 项目说明
├── .editorconfig          # 编辑器统一配置
├── docker-compose.yml     # 本地全栈编排
├── docs/                  # 开发工作流、提交规范、验证清单、测试策略等
├── scripts/               # check.sh + validate-*.sh 一键质量检查
├── .github/               # workflows（ci/cd-*/release）+ PR 模板
├── vocata-server/         # 后端服务（Spring Boot，端口 9009）
├── vocata-web/            # 用户端前端（Vue 3 + TS + Vite，端口 3000）
└── vocata-admin/          # 管理端前端（Vue 3 + TS + Tailwind，端口 3001）
```

三个前后端模块**均已建成**，不是规划中。

## 后端结构（vocata-server）

### 顶层
```
vocata-server/
├── src/main/java/com/vocata/   # Java 源码
├── src/main/resources/         # application*.yml 等配置
├── src/test/java/              # 测试代码
├── pom.xml                     # Maven 配置
└── Dockerfile
```

启动类：`com.vocata.VocataApplication`（**不是** VocataServerApplication）。

### 业务模块（10 个）
```
com.vocata/
├── VocataApplication.java
├── common/        # config constant controller entity exception handler result utils
│                  #   ├── entity/BaseEntity.java（部分实体继承，非全部）
│                  #   ├── result/{ApiResponse, ApiCode, PageResult}
│                  #   ├── exception/{BizException, GlobalExceptionHandler}
│                  #   └── utils/{UserContext, ...}
├── config/        # 7 个配置类（见下）
├── auth/          # constants controller dto entity service（注册/登录/邮箱验证/密码重置）
├── user/          # constants controller dto entity mapper service（含 UserFavorite 收藏）
├── character/     # constants controller dto entity mapper service task（角色 CRUD/标签/AI 生成）
├── conversation/  # constants controller dto entity mapper service（会话/消息/自动标题）
├── ai/            # config controller dto llm pipeline response service stt tts websocket
├── voice/         # controller dto entity mapper service（TTS 音色管理）
├── file/          # config constants controller dto service（七牛云上传）
└── admin/         # controller dto service（管理端鉴权/用户管理/音色管理）
```

**没有** `favorite/` 或 `search/` 独立模块：收藏属于 `user`（实体 `UserFavorite`），无搜索模块。

### config 模块真实内容（7 个）
```
config/
├── MybatisPlusConfig.java   # 分页/逻辑删除
├── SaTokenConfig.java       # 路由鉴权
├── RedisConfig.java         # Lettuce/Redisson
├── WebConfig.java
├── WebMvcConfig.java        # /api 前缀、拦截器
├── WebClientConfig.java     # WebFlux WebClient（第三方 API）
└── WebSocketConfig.java     # AI 流式聊天 WebSocket
```

### ai 模块子包（AI 流式核心）
```
ai/
├── llm/         # LlmProvider 抽象 + Qiniu/OpenAi/Gemini/SiliconFlow 实现
├── stt/         # SttClient → Qiniu/Xunfei
├── tts/         # TtsClient → Volcan/Xunfei
├── pipeline/    # STT → LLM → TTS 编排
├── websocket/   # AiChatWebSocketHandler（fragile core，改动需 ask-first）
├── response/    # 流式响应封装
├── controller/ service/ dto/ config/
```

### 资源文件（真实）
```
src/main/resources/
├── application.yml                 # 默认 active profile = local
├── application-local.yml.template  # 本地配置模板（实际 application-local.yml 由本地生成/不入库）
├── application-test.yml
├── application-prod.yml
├── logback-spring.xml
└── static/
```

**注意**：项目**无 SQL 迁移**（无 `db/migration/`、无 `.sql` 文件），Mapper 用 **MyBatis 注解**（`@Select` 等）而非 XML 映射文件。建表方式见 `.ai-rules/database.md`。

## 前端结构

两个前端都是 **Vue 3 + TypeScript + Vite**（`<script setup>` + Composition API + Pinia + Vue Router 4 + Axios）。入口是 `src/main.ts`，配置文件是 `.ts`（非 `.js`）。详细规范见 `.ai-rules/frontend.md`。

### 用户端（vocata-web）
```
vocata-web/
├── index.html
├── vite.config.ts / vitest.config.ts
├── eslint.config.ts          # ESLint 9 flat config（非 .eslintrc.js）
├── tsconfig*.json / env.d.ts
├── package.json
└── src/
    ├── main.ts  App.vue
    ├── api/                  # Axios 实例 + 按模块接口
    ├── assets/               # 样式（SCSS）/图片
    ├── components/
    ├── composables/          # 组合式函数
    ├── layouts/  router/  store/  views/
    ├── types/                # TS 类型定义
    └── tests/                # Vitest 单测（如 avatar.spec.ts）
```

### 管理端（vocata-admin）
```
vocata-admin/
├── vite.config.ts  eslint.config.ts  tsconfig*.json
└── src/
    ├── main.ts  App.vue
    ├── api/  layouts/  router/  store/  utils/  views/  types/
```
管理端额外使用 **Tailwind CSS 4**（`@tailwindcss/vite` + `postcss-pxtorem` 移动端 rem 适配）；用户端使用 SCSS，**不用** Tailwind。两端 token 存储依赖 `js-cookie`。

### 前端脚本差异（已核实）
- 公共：`dev` `build` `build:local/test/prod` `preview` `type-check`（`vue-tsc`）`lint` `lint:fix` `format` `format:check` `check`（`run-s` 串联）。
- **仅 vocata-web 有**：`test` / `test:watch`（Vitest）。管理端暂无前端单测脚本。

## 模块与命名约定

### 后端分层（强约束）
```
Controller → Service(impl) → Mapper → PostgreSQL/Redis
```
- Controller：只处理 HTTP，不含业务逻辑。
- Service：业务逻辑，接口 + `impl/` 实现。
- Mapper：纯数据访问（MyBatis 注解）。
- Entity：数据库实体，**部分**继承 `BaseEntity`（`User`/`Conversation`/`Message` 继承；`Character`/`UserFavorite`/`CharacterTag` 不继承——先看具体类）。
- DTO：API 输入输出，与 Entity 分离；复杂模块用 `dto/request`、`dto/response` 子目录（如 conversation）。

### 命名
- 包名：`com.vocata.{module}.{layer}`，模块名单数（`user`/`character`），**层级名单数**（`controller`/`service`/`mapper`，**不是**复数）。
- 类名 PascalCase：`{Module}Controller` / `{Module}Service` / `{Module}ServiceImpl` / `{Module}Mapper` / `{Module}.java`（实体）。DTO 按用途：`LoginRequest`、`UserResponse`。
- 方法 camelCase：CRUD 用 `create/get/update/delete`，查询 `findByXxx`/`listByXxx`。
- 常量全大写下划线，归入模块 `constants/`。
- **ID 字段返回前端一律 String**（`String.valueOf(id)`，防 JS 精度丢失）。

### 配置优先级
环境变量 > `application-{profile}.yml` > `application.yml`（默认 `active: local`）。

