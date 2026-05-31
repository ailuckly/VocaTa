---
title: Technical Architecture
description: "定义项目的技术栈、架构模式和开发规范。"
inclusion: always
---

# VocaTa技术架构文档

> 架构总览。编码细则见 `.ai-rules/backend.md` / `frontend.md`，数据库见 `.ai-rules/database.md`，结构见 `.ai-rules/structure.md`，协作规则见 `AGENTS.md`。

## 技术栈概览

### 后端核心技术
- **Java 17** + **Spring Boot 3.1.4**
- **MyBatis Plus 3.5.3.2** - ORM（注解 SQL，无 XML 映射）
- **Sa-Token 1.37.0** - 权限认证
- **PostgreSQL**（JDBC 42.7.11） - 关系型数据库
- **Redis**（Lettuce + Redisson 3.23.4） - 缓存/会话/分布式锁
- **Spring WebFlux `WebClient`** - 非阻塞调用第三方 STT/LLM/TTS
- **Spring WebSocket / Tyrus** - AI 流式聊天
- **spring-boot-starter-mail** - 注册邮箱验证
- **七牛云 SDK / 科大讯飞语音 SDK / BCrypt / Hutool 5.8.22**

### 前端核心技术
- **Vue 3 + TypeScript**（`<script setup>` + Composition API）
- **Vite 7** 构建，入口 `main.ts`，配置为 `.ts`
- **Element Plus** UI、**Pinia**（setup store）、**Vue Router 4**、**Axios**
- 样式：**vocata-web 用 SCSS/BEM**；**vocata-admin 用 Tailwind CSS 4**
- **ESLint 9（flat config）+ Prettier 3**；测试 **Vitest**（仅 web）

### 开发工具
- **Maven**（阿里云镜像）、**Docker**、**HuTool**

## 架构设计模式

### 1. 分层架构（Layered Architecture）
```
┌─────────────────────────────────────┐
│           Controller Layer          │ <- REST API端点
├─────────────────────────────────────┤
│            Service Layer            │ <- 业务逻辑层
├─────────────────────────────────────┤
│            Mapper Layer             │ <- 数据访问层
├─────────────────────────────────────┤
│           Database Layer            │ <- PostgreSQL + Redis
└─────────────────────────────────────┘
```

### 2. 模块化设计
每个业务模块遵循标准目录结构：
```
com.vocata.{module}/
├── controller/     # REST端点控制器
├── service/        # 业务逻辑服务层
├── mapper/         # MyBatis数据访问层
├── entity/         # 数据库实体类
└── dto/           # 数据传输对象
```

### 3. 统一响应格式
所有API返回统一的`ApiResponse<T>`格式：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1634567890123
}
```

关于返回ID字段时都需要返回string类型给前端

## 核心架构组件

### 1. 认证授权架构
- **Sa-Token框架**：轻量级JWT认证
- **用户上下文**：`UserContext`线程本地存储
- **权限拦截**：基于路由的权限控制
- **多环境支持**：开发/测试/生产环境隔离

### 2. 数据库架构
- **基础实体**：**部分**实体继承 `BaseEntity`（`User`/`Conversation`/`Message` 继承；`Character`/`UserFavorite`/`CharacterTag` 不继承），改动前先看具体类
- **审计字段**：自动填充创建人/时间、更新人/时间（**字段名不全表统一**，如 `vocata_character` 用 `created_at`/`updated_at`）
- **逻辑删除**：使用`@TableLogic`软删除
- **ID策略**：雪花算法生成分布式 ID，列类型 `BIGINT`
- **命名规范**：表名 **`vocata_`** 前缀（**不是** `tb_`），字段下划线命名
- 详见 `.ai-rules/database.md`

### 3. 异常处理架构
- **全局异常处理**：`GlobalExceptionHandler`统一处理
- **业务异常**：`BizException`业务逻辑异常
- **错误码管理**：`ApiCode`枚举定义所有错误状态
- **异常响应**：自动转换为统一响应格式

### 4. 配置管理
- **多环境配置**：`application-{profile}.yml`（默认 `active: local`）
- **环境变量**：敏感配置通过环境变量注入（未用 spring-dotenv）
- **配置优先级**：环境变量 > 配置文件 > 默认值

### 5. AI 流式架构（项目核心）

语音链路 **STT → LLM → TTS**，文本聊天走 WebSocket 流式（`AiChatWebSocketHandler`）：

- **LLM 抽象**：`LlmProvider` 接口 + `QiniuLlmProvider` / `OpenAiLlmProvider` / `GeminiLlmProvider` / `SiliconFlowLlmProvider`
- **STT**：`SttClient` → `QiniuSttClient` / `XunfeiWebSocketSttClient`
- **TTS**：`TtsClient` → `VolcanTtsClient` / `XunfeiStreamTtsClient`
- **编排**：`AiStreamingService` 处理流式响应，`AiPromptEnhanceService` 做 prompt 优化
- **非阻塞 IO**：第三方 API 用 WebFlux `WebClient`（`WebClientConfig`），WebSocket 配置在 `WebSocketConfig`
- **fragile core**：`ai/` 流式链路、WebSocket、provider 接线属高风险区，改动需 ask-first（见 `AGENTS.md`）

## 开发和运行命令

### 本地开发
```bash
# 启动后端服务 (端口9009)
cd vocata-server
mvn spring-boot:run

# 指定环境启动
mvn spring-boot:run -Dspring-boot.run.profiles=test

# 构建JAR包
mvn clean package
```

### 容器化部署
```bash
# 构建Docker镜像
docker build -t vocata-server .

# 运行容器
docker run -p 9009:9009 vocata-server
```

## PostgreSQL 数据库设计规范

数据库设计、真实表清单、字段类型、Mapper 注解 SQL 转义规则等，以 **`.ai-rules/database.md`** 为权威源。要点摘录：

- 表名 `vocata_` 前缀，关联表 `_relation` 结尾，字段下划线命名。
- 主键 `BIGINT` + 应用层雪花 ID（**非 `BIGSERIAL`**）。
- 禁用 ENUM（用 `SMALLINT`）、禁用物理外键（用关联表）、JSON 用 `JSONB`、时间用 `TIMESTAMP WITH TIME ZONE`。
- 多数表有 `create_id`/`update_id`/`create_date`/`update_date`/`is_delete` 审计字段 + `@TableLogic` 软删除，但**字段名不全表统一**，以实体类为准。
- **真实表共 11 张**（`vocata_user`、`vocata_character`、`vocata_messages`、`vocata_conversations`、`vocata_character_tag`、`vocata_tag`、`vocata_tag_stats`、`vocata_user_favorite`、`vocata_tts_voices`、`vocata_voice_profile`、`vocata_login_log`）。**不存在** `vocata_favorite`/`vocata_admin`/`vocata_ai_service`/`vocata_search_history` 等表（收藏在 `vocata_user_favorite`）。

## API设计规范

### 路由设计
- **客户端API**：`/api/client/**` （部分需认证）
- **管理端API**：`/api/admin/**` （仅管理员）
- **公开API**：`/api/open/**` （无需认证）

### 请求响应格式
- 请求：使用DTO对象，支持JSR-303验证
- 响应：统一`ApiResponse<T>`包装
- 分页：返回`PageResult<T>`格式
- 错误：返回标准错误码和消息

## 测试策略

### 单元测试
- Service层业务逻辑测试
- Mapper层数据访问测试
- 工具类和辅助方法测试

### 集成测试
- Controller层API测试
- 数据库集成测试
- 外部服务集成测试

## 性能和监控

### 缓存策略
- Redis缓存热点数据
- Sa-Token会话缓存
- 数据库查询结果缓存

### 连接池配置
- HikariCP数据库连接池
- Redis连接池优化
- 合理的超时和重试设置

### 日志记录
- 结构化日志输出
- 不同环境的日志级别
- 关键业务操作审计日志