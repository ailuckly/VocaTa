# AI 协作流程

这份文档把 AI coding agent 的工作方式落到仓库流程里。目标不是让 AI 更自由，
而是让 AI 的改动可追踪、可验证、可审查。

## 适用范围

- 需求拆解、技术方案、重构计划
- 后端、前端、Admin、Docker、CI、文档改动
- Issue triage、PR 准备、Review 修复
- 已有代码的解释、风险审计和测试补强

涉及密钥、生产配置、数据库 schema、鉴权、部署、WebSocket 实时链路或第三方 AI provider
时，AI agent 必须先说明风险边界，再进入实现。

## 标准节奏

1. 确认当前分支和工作区状态。
2. 阅读相关模块、`.ai-rules/`、`AGENTS.md`、`CONTRIBUTING.md` 和邻近实现。
3. 把目标拆成可验证的最小改动。
4. 对功能和 bugfix 优先补测试或给出无法测试的原因。
5. 小步修改，不覆盖用户已有改动。
6. 按改动范围运行验证脚本。
7. 总结改动、验证命令、失败项和剩余风险。

准备 PR 或交付摘要前，可以先运行 `./scripts/pre-pr-check.sh` 获取本次改动的建议验证范围。

## Issue 到 PR

### Issue Intake

- Bug：复现步骤、期望行为、实际行为、日志、影响范围。
- Feature：用户问题、最小方案、替代方案、风险和回滚。
- AI agent task：目标、上下文、必须完成、不在范围、高风险边界和验证计划。
- Security：不要公开 issue，按 `SECURITY.md` 私下报告。

### 方案阶段

AI agent 应明确：

- 改动边界：backend / web / admin / Docker / CI / docs
- 是否触碰高风险路径：auth、data write、WebSocket streaming、provider wiring、deployment
- 需要新增或更新哪些测试
- 验证命令和验收标准

### 实现阶段

- 保持一个 PR 一个主题。
- 不混入无关格式化、依赖升级或重构。
- 不引入 Lombok、额外 hook 工具或重型依赖，除非任务明确要求并已说明取舍。
- API ID 继续按前端安全精度要求处理为字符串。
- `.env`、provider key、token、私钥和生产配置不得进入提交。

### Review 阶段

Review 优先级：

1. 安全：鉴权、token、密钥、注入、敏感错误输出。
2. 正确性：边界条件、事务、并发、空值、异常路径。
3. API 兼容性：返回结构、ID 类型、路由语义。
4. 可靠性：超时、重试、回滚、幂等性。
5. 测试缺口：新增行为、失败路径、回归覆盖。

AI agent 做 review 或实现后自查时，使用 `docs/AI审查清单.md` 统一输出 findings、
open questions、test gaps 和 summary。

## 验证矩阵

| 改动范围 | 必跑命令 |
| --- | --- |
| 文档 / 模板 / 元数据 | `./scripts/validate-docs.sh` |
| 后端 | `./scripts/validate-backend.sh` |
| 用户端 | `./scripts/validate-web.sh` |
| 管理端 | `./scripts/validate-admin.sh` |
| 前端产物体积 | `./scripts/report-frontend-assets.sh` |
| Docker / 环境变量 | `./scripts/validate-docker.sh` |
| 跨模块或发布前 | `./scripts/check.sh`，必要时补 Docker / staging 验证 |

如果验证无法运行，PR 或任务总结必须写明：

- 未运行的命令
- 失败原因
- 仍未覆盖的风险

## 交付摘要格式

AI agent 结束一次任务时应提供：

- 改了什么
- 验证了什么，列出真实命令和结果
- 没验证什么，原因是什么
- 剩余风险和建议后续项

## 相关入口

- `AGENTS.md`
- `CONTRIBUTING.md`
- `CODE_STYLE.md`
- `SECURITY.md`
- `.github/pull_request_template.md`
- `.github/ISSUE_TEMPLATE/`
- `docs/AI任务模板.md`
- `docs/AI审查清单.md`
- `docs/adr/README.md`
- `docs/开源成熟度路线图.md`
- `docs/开源成熟度审计.md`
- `docs/数据库迁移基线方案.md`
- `docs/依赖与安全自动化方案.md`
- `docs/发布流程硬化方案.md`
- `docs/AI核心链路测试夹具方案.md`
- `docs/维护与分流指南.md`
- `docs/发布检查清单.md`
- `docs/开发工作流.md`
- `docs/验证清单.md`
