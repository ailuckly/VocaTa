# VocaTa

[![CI](https://github.com/ailuckly/VocaTa/actions/workflows/ci.yml/badge.svg)](https://github.com/ailuckly/VocaTa/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 17](https://img.shields.io/badge/Java-17-blue.svg)](vocata-server/pom.xml)
[![Node.js](https://img.shields.io/badge/Node.js-20.19%2B%20%7C%2022.12%2B%20%7C%2024%2B-green.svg)](scripts/check-node-version.sh)

一个实时语音 AI 角色对话项目。

它不只是“发消息 + 播语音”，而是尽量把对话做得更像真人交流：你可以开口说话、随时打断、继续追问，角色会用自己的语气持续接话。

> 实时说话、实时接话、实时打断。
> 让 AI 角色聊天从“消息收发”变成更接近真人的互动体验。

## 一句话感受

`像在和角色通电话，而不是在等一个聊天机器人回复。`

## 小亮点

| 能力 | 描述 |
| --- | --- |
| 流式回复 | 回复不是整段憋出来，而是边生成边出来 |
| 语音打断 | AI 说到一半，可以直接插话 |
| 角色感 | 不同角色可以有不同性格、语气和说话方式 |
| 多轮对话 | 能连续聊，不是一轮就断 |
| 多模型接入 | STT / LLM / TTS 可以灵活切换组合 |

## 它可以做什么

- 和 AI 角色进行实时语音对话
- 支持文本聊天和语音聊天两种模式
- 角色回复支持流式输出，不用整段生成完再看到结果
- 语音回复支持边生成边播放，等待感更低
- 用户插话时可以直接打断 AI，不必等它说完
- 支持多轮连续聊天，保留会话上下文
- 支持角色创建与角色设定管理
- 支持根据不同角色切换不同语气、性格和说话风格
- 支持历史会话查看与消息持久化
- 支持多种 STT / TTS / LLM 服务接入与切换

## 项目体验感

这个项目想做出来的感觉是：

- AI 不要“慢半拍”
- 对话不要“一问一答像接口调用”
- 角色不要每轮都像失忆
- 用户说到一半时，AI 应该能被打断
- 回复应该更像聊天，而不是一整段说明文

## 你会看到的效果

### 说话更顺

- 用户开口后，系统开始持续识别
- 模型生成到哪，文本就显示到哪
- 语音不必等全文结束再统一播报

### 互动更真

- AI 在“说话”的时候，用户可以直接插进来
- 对话节奏更像来回接话，而不是轮流提交任务
- 连续追问时不会那么容易掉出聊天状态

### 角色更像角色

- 不同角色可以有不同人设与说话口吻
- 同一句话，不同角色能聊出不一样的感觉
- 更适合做陪伴感、代入感、故事感比较强的对话

## 功能亮点

### 实时语音聊天

- 音频输入后可持续识别文本
- 模型回复按文本流返回
- 语音合成按句子切片输出
- 首段回复更快出来，聊天节奏更自然

### 全双工打断

- AI 正在说话时，用户可以重新开口
- 新输入到来后，旧语音可以立即停止
- 避免“用户已经插话，AI 还在继续念稿”

### 角色扮演

- 每个角色都可以配置独立人设
- 支持性格、说话风格、示例对话等设定
- 更适合做陪伴、故事代入、角色闲聊这类场景

### 多模型接入

- 支持不同 LLM 服务切换
- 支持不同 STT / TTS 服务接入
- 更方便根据速度、效果、成本做组合

### 会话管理

- 支持角色会话列表
- 支持历史消息保存
- 支持持续多轮聊天

## 适合的使用场景

- 想做一个能“开口聊”的 AI 角色陪伴应用
- 想体验比普通聊天框更自然的实时语音交互
- 想做二次元角色、原创 OC、虚拟人、故事角色对话
- 想把 `STT + LLM + TTS` 串成完整语音链路
- 想验证流式输出、语音打断、多轮上下文这类能力

## 项目关键词

`Realtime Voice Chat` `AI Roleplay` `Streaming Response` `Barge-in` `WebSocket` `STT` `LLM` `TTS`

## 项目状态

VocaTa 处于活跃开发阶段，当前以 `develop` 为主要集成分支，功能和部署流程仍在持续收敛。仓库已经补齐基础开源协作入口和 AI agent 工作流，但还没有正式 release 版本和数据库迁移基线；新环境初始化仍可能需要可信备份或手动 schema 准备。

后续成熟度路线见 [`docs/开源成熟度路线图.md`](docs/开源成熟度路线图.md)。

## 仓库结构

```text
.
├── vocata-server   # 后端服务
├── vocata-web      # 用户端前端
├── vocata-admin    # 管理后台
├── docs            # 开发与部署文档
├── docker-compose.yml
├── docker-compose.test.yml
└── docker-compose.prod.yml
```

## 技术栈

- 后端：`Spring Boot 3`、`Java 17`、`WebSocket`、`Reactor`
- 前端：`Vue 3`、`TypeScript`、`Vite`
- 数据库：`PostgreSQL`、`Redis`
- AI 能力：`STT`、`LLM`、`TTS`

## 快速启动

### 环境要求

- Java `17`
- Maven
- Node.js `20.19+`、`22.12+` 或 `24+`
- Docker / Docker Compose

仓库根目录提供 `.java-version` 和 `.nvmrc`。使用 jenv/asdf 的环境可以按 `.java-version`
切到 Java 17；使用 nvm 的环境可以执行 `nvm use` 切到推荐 Node 版本。

### 准备环境变量

```bash
cp .env.example .env
```

`.env` 是本地私有配置文件，可能包含真实密钥，必须保持未跟踪状态。不要提交、截图或粘贴 `.env` 内容。

### 推荐：宿主机开发模式

这个模式最适合日常开发：PostgreSQL / Redis 用 Docker，后端和两个前端在本机运行，支持热更新。

#### 1. 启动基础依赖

```bash
docker compose up -d postgres redis
```

#### 2. 启动后端

后端本机运行时不会自动读取根目录 `.env`，需要先导入环境变量：

```bash
set -a
source .env
set +a
cd vocata-server
mvn spring-boot:run
```

如果本机 Maven 仓库不可写，使用项目验证时常用的临时仓库：

```bash
cd vocata-server
mvn -Dmaven.repo.local=/tmp/juhao_m2repo spring-boot:run
```

#### 3. 启动用户端

```bash
cd vocata-web
npm install
npm run dev
```

#### 4. 启动管理后台

```bash
cd vocata-admin
npm install
npm run dev
```

访问地址：

- 用户端：`http://localhost:3000`
- 管理后台：`http://localhost:3001`
- 后端健康检查：`http://localhost:9009/api/health`

### 可选：全量 Docker 模式

这个模式用于检查容器构建和近似发布态静态前端，不提供前端热更新：

```bash
docker compose up -d --build
```

全量 Docker 模式会使用 `.env` 中的 `DOCKER_DB_HOST`、`DOCKER_DB_PORT`、`DOCKER_REDIS_HOST`、`DOCKER_REDIS_PORT` 连接 compose 内部服务。

### 数据库说明

当前仓库还没有正式的 schema migration 体系。新建空库后，服务可能能启动，但业务接口会因为缺少表结构或种子数据失败。现阶段请使用已有开发库/备份恢复；后续应补 Flyway/Liquibase 或独立 SQL 初始化脚本。

如果需要为后续 migration baseline 准备可信 schema，可以先运行 `./scripts/export-schema.sh`，它只导出 schema-only DDL，不会导出业务数据。默认输出到 `.local/database-schema-baseline.sql`，该路径仅用于本地审查准备，不应直接提交；确认后的 baseline SQL 应在单独 PR 中进入正式 migration 目录。

## 常用命令

### 后端

```bash
cd vocata-server
mvn spring-boot:run
mvn -Dmaven.repo.local=/tmp/juhao_m2repo spring-boot:run
mvn test
mvn clean package -DskipTests
```

### 用户端

```bash
cd vocata-web
npm run dev
npm run lint
npm run type-check
npm run test
npm run build
npm run check
```

### 管理后台

```bash
cd vocata-admin
npm run dev
npm run lint
npm run type-check
npm run build
npm run check
```

### 本地验证

```bash
./scripts/doctor.sh
./scripts/doctor.sh --json
./scripts/check.sh
./scripts/pre-pr-check.sh
./scripts/report-frontend-assets.sh
./scripts/validate-docs.sh
./scripts/validate-backend.sh
./scripts/validate-web.sh
./scripts/validate-admin.sh
./scripts/validate-docker.sh
```

说明：`./scripts/doctor.sh` 用于只读检查本机开发环境，支持 `./scripts/doctor.sh --json` 输出机器可读摘要，但不会打印环境变量值。`./scripts/check.sh` 是根目录一键质量检查入口，会依次运行文档/模板/元数据检查、后端验证、用户端验证和管理端验证。`./scripts/pre-pr-check.sh` 会根据当前变更范围提示 PR 前应运行哪些验证；`./scripts/report-frontend-assets.sh` 用于查看已有前端 production build asset 体积，支持 `--json` 机器可读输出，不会自动构建，也不是失败门禁。Docker 编排检查仍保留为单独命令，因为它依赖本机 Docker 环境。

## 开发规范

- AI 协作指南：[`AGENTS.md`](AGENTS.md)
- 贡献流程：[`CONTRIBUTING.md`](CONTRIBUTING.md)
- 代码风格：[`CODE_STYLE.md`](CODE_STYLE.md)
- AI 协作流程：[`docs/AI协作流程.md`](docs/AI协作流程.md)
- AI 任务模板：[`docs/AI任务模板.md`](docs/AI任务模板.md)
- AI 审查清单：[`docs/AI审查清单.md`](docs/AI审查清单.md)
- 架构决策记录：[`docs/adr/README.md`](docs/adr/README.md)
- 版本发布策略：[`docs/版本发布策略.md`](docs/版本发布策略.md)
- 开发工作流：[`docs/开发工作流.md`](docs/开发工作流.md)
- 提交规范：[`docs/提交规范.md`](docs/提交规范.md)
- 验证清单：[`docs/验证清单.md`](docs/验证清单.md)
- 测试策略：[`docs/测试策略.md`](docs/测试策略.md)
- 技术债与后续工作：[`docs/技术债与后续工作.md`](docs/技术债与后续工作.md)
- AI 核心链路测试夹具方案：[`docs/AI核心链路测试夹具方案.md`](docs/AI核心链路测试夹具方案.md)

## 开源协作

- License：[`LICENSE`](LICENSE)
- 安全策略：[`SECURITY.md`](SECURITY.md)
- 支持说明：[`SUPPORT.md`](SUPPORT.md)
- 维护者说明：[`MAINTAINERS.md`](MAINTAINERS.md)
- 行为准则：[`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md)
- 变更记录：[`CHANGELOG.md`](CHANGELOG.md)
- 版本发布策略：[`docs/版本发布策略.md`](docs/版本发布策略.md)
- 维护与分流指南：[`docs/维护与分流指南.md`](docs/维护与分流指南.md)
- 开源成熟度路线图：[`docs/开源成熟度路线图.md`](docs/开源成熟度路线图.md)
- 开源成熟度审计：[`docs/开源成熟度审计.md`](docs/开源成熟度审计.md)
- 技术债与后续工作：[`docs/技术债与后续工作.md`](docs/技术债与后续工作.md)
- 数据库迁移基线方案：[`docs/数据库迁移基线方案.md`](docs/数据库迁移基线方案.md)
- 依赖与安全自动化方案：[`docs/依赖与安全自动化方案.md`](docs/依赖与安全自动化方案.md)
- 发布流程硬化方案：[`docs/发布流程硬化方案.md`](docs/发布流程硬化方案.md)
- AI 核心链路测试夹具方案：[`docs/AI核心链路测试夹具方案.md`](docs/AI核心链路测试夹具方案.md)
- PR 模板：[`.github/pull_request_template.md`](.github/pull_request_template.md)
- Issue 模板：[`.github/ISSUE_TEMPLATE/`](.github/ISSUE_TEMPLATE/)
- Git 文本规范：[`.gitattributes`](.gitattributes)

## 相关文档

- 开发环境说明：[`docs/开发环境说明.md`](docs/开发环境说明.md)
- Docker 开发环境：[`docs/Docker开发环境.md`](docs/Docker开发环境.md)
- AI 协作流程：[`docs/AI协作流程.md`](docs/AI协作流程.md)
- AI 任务模板：[`docs/AI任务模板.md`](docs/AI任务模板.md)
- AI 审查清单：[`docs/AI审查清单.md`](docs/AI审查清单.md)
- 架构决策记录：[`docs/adr/README.md`](docs/adr/README.md)
- 开源成熟度路线图：[`docs/开源成熟度路线图.md`](docs/开源成熟度路线图.md)
- 发布检查清单：[`docs/发布检查清单.md`](docs/发布检查清单.md)
- 版本发布策略：[`docs/版本发布策略.md`](docs/版本发布策略.md)
- 维护与分流指南：[`docs/维护与分流指南.md`](docs/维护与分流指南.md)
- 开源成熟度审计：[`docs/开源成熟度审计.md`](docs/开源成熟度审计.md)
- 技术债与后续工作：[`docs/技术债与后续工作.md`](docs/技术债与后续工作.md)
- 数据库迁移基线方案：[`docs/数据库迁移基线方案.md`](docs/数据库迁移基线方案.md)
- 依赖与安全自动化方案：[`docs/依赖与安全自动化方案.md`](docs/依赖与安全自动化方案.md)
- 发布流程硬化方案：[`docs/发布流程硬化方案.md`](docs/发布流程硬化方案.md)
- AI 核心链路测试夹具方案：[`docs/AI核心链路测试夹具方案.md`](docs/AI核心链路测试夹具方案.md)
- 部署环境说明：[`docs/部署环境说明.md`](docs/部署环境说明.md)
- 验证清单：[`docs/验证清单.md`](docs/验证清单.md)
- 测试策略：[`docs/测试策略.md`](docs/测试策略.md)
- 开发工作流：[`docs/开发工作流.md`](docs/开发工作流.md)
- 提交规范：[`docs/提交规范.md`](docs/提交规范.md)
