# AI 任务模板

复制本模板到 issue、PR 描述或任务说明中，用于约束 AI coding agent 的工作边界。

如果任务在 GitHub issue 中提出，也可以直接使用 `.github/ISSUE_TEMPLATE/ai_task.yml`。

## 背景

- 问题或目标：
- 相关 issue / PR：
- 当前分支：
- 相关模块：backend / vocata-web / vocata-admin / Docker / CI / docs

## 范围

### 必须完成

-

### 不在本次范围

-

### 高风险边界

勾选本次是否涉及：

- [ ] 鉴权 / Sa-Token / `UserContext`
- [ ] 数据库 schema / 数据迁移 / `vocata_*` 表结构
- [ ] 数据写入或删除
- [ ] API 响应契约
- [ ] `AiChatWebSocketHandler` 或 STT -> LLM -> TTS 链路
- [ ] 第三方 AI provider 接线
- [ ] Docker / CI / CD / 部署配置
- [ ] 新增、移除或升级依赖

如涉及以上任一项，先写风险和回滚方案，再实现。

## 实现要求

- 遵守 `AGENTS.md`、`.ai-rules/`、`CODE_STYLE.md` 和 `docs/AI协作流程.md`。
- 保持改动聚焦，不混入无关格式化或顺手重构。
- 不提交 `.env`、密钥、token、私钥、生产配置或敏感日志。
- 功能和 bugfix 优先补测试；无法补测试时说明原因。

## 验证计划

按改动范围勾选并实际运行：

- [ ] `git diff --check`
- [ ] `./scripts/validate-backend.sh`
- [ ] `./scripts/validate-web.sh`
- [ ] `./scripts/validate-admin.sh`
- [ ] `./scripts/validate-docker.sh`
- [ ] `./scripts/check.sh`
- [ ] 其他：

## 交付摘要

完成后必须说明：

- 改了什么
- 跑了哪些命令，真实结果是什么
- 哪些命令没跑，原因是什么
- 剩余风险和后续建议

如本任务包含 review 或实现后自查，按 `docs/AI审查清单.md` 输出 findings、test gaps
和剩余风险。

## 决策记录

如本任务改变认证、数据写入、API 契约、WebSocket streaming、AI provider 接线、部署、
CI、依赖、数据库基线或发布策略，考虑补充 ADR：`docs/adr/README.md`。
