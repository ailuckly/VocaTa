# Contributing

这份文档定义 VocaTa 的最小协作流程。目标是让个人开发、代码审查、CI 检查和 AI coding agent 协作都能沿着同一套节奏推进。

## 本地启动流程

### 1. 准备基础依赖

```bash
docker compose up -d postgres redis
```

如需一次性启动完整本地环境：

```bash
docker compose up -d
```

### 2. 启动后端

```bash
cd vocata-server
mvn spring-boot:run
```

后端默认端口为 `9009`，本地默认 profile 当前由 `application.yml` 设置为 `local`。

### 3. 启动用户端

```bash
cd vocata-web
npm install
npm run dev
```

用户端默认端口为 `3000`。

### 4. 启动管理后台

```bash
cd vocata-admin
npm install
npm run dev
```

管理后台默认端口为 `3001`。

## 分支命名规范

从 `develop` 拉出工作分支，优先使用以下命名：

- `feature/xxx`：新增功能
- `fix/xxx`：缺陷修复
- `refactor/xxx`：重构
- `docs/xxx`：文档变更
- `chore/xxx`：维护、脚本、配置等杂项

一个分支只处理一个主题。不要在功能分支中混入无关修复、格式化或部署改动。

## 开发流程

1. 从 `develop` 更新最新代码。
2. 创建语义清晰的工作分支。
3. 修改前先阅读相关模块、现有测试、`.ai-rules/` 和 `AGENTS.md`。
4. 小步提交，保持每个 commit 可独立解释。
5. 按改动范围运行本地检查。
6. 提交 PR 到 `develop`。
7. 等待 CI 和 Review 通过后再合并。

## 提交规范

提交格式：

```text
<type>: <summary>
```

允许的 `type`：

- `feat`：新增功能
- `fix`：修复缺陷
- `refactor`：重构且不改变外部行为
- `docs`：文档变更
- `test`：测试变更
- `chore`：维护性改动
- `style`：纯格式调整
- `perf`：性能优化
- `build`：构建系统或依赖变更
- `ci`：CI 配置变更
- `revert`：回滚提交

提交要求：

- 一个 commit 只做一件事。
- summary 使用英文动词短语，例如 `fix login redirect`。
- 不提交无关改动。
- 不提交临时调试代码。
- 大改动拆成多个可验证 commit。

更多示例见 [`docs/提交规范.md`](docs/提交规范.md)。

## 本地验证

按修改范围运行对应命令：

```bash
./scripts/check.sh
./scripts/validate-backend.sh
./scripts/validate-web.sh
./scripts/validate-admin.sh
./scripts/validate-docker.sh
```

推荐在提交前至少运行：

```bash
git diff --check
```

当前验证脚本说明：

- `check.sh` 执行空白检查、后端验证、用户端验证和管理端验证。
- 后端脚本执行 Maven package baseline 和 test baseline。
- 用户端脚本执行 ESLint、TypeScript type-check、Vitest、production build。
- 管理端脚本执行 ESLint、TypeScript type-check、production build。
- Docker 脚本检查 Compose 配置和当前服务状态。

## PR 流程

PR 默认合入 `develop`。PR 描述默认使用中文，英文只在团队或上下游协作需要时使用。

PR 描述必须说明：

- 概要：做了什么。
- 背景：为什么要做。
- 主要改动：改了哪些重点。
- 测试：跑了哪些检查。
- 风险：风险和回滚思路。
- 截图 / 演示：涉及 UI 时提供截图或演示说明。

## Git Hook 工具取舍

当前不引入 `commitlint`、`husky` 或 `lint-staged`。

优点：

- 可以更早拦截不规范 commit。
- 可以在提交前自动跑格式化或 lint。
- 多人协作时能减少 Review 中的格式噪音。

缺点：

- 会新增依赖和 hook 维护成本。
- 可能影响临时修复、跨平台环境和 AI agent 执行流程。
- 现阶段会和已有 CI、PR 模板、`./scripts/check.sh` 形成重复约束。

当前判断：个人项目优先保持简单，先依靠文档规范、PR checklist、CI 和本地检查脚本。后续如果协作者增多或提交格式频繁失控，再重新评估引入。

## Review Checklist

提交 Review 前请确认：

- 改动范围和 PR 主题一致。
- lint 通过，或明确说明未运行原因。
- typecheck 通过，或明确说明未运行原因。
- test 通过，或明确说明当前测试缺口。
- build 通过，或明确说明未运行原因。
- 没有提交密钥、密码、token、真实生产配置。
- 没有引入不必要的新依赖。
- API 响应、路由鉴权、数据库写入和错误处理已自查。
- 涉及 UI 的改动已检查基础响应式表现。
