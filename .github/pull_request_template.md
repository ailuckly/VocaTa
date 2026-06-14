## 概要

<!-- 简要说明这个 PR 做了什么。默认使用中文描述。 -->

## 背景

<!-- 说明为什么需要这个改动，它解决了什么问题。 -->

关联 Issue：

- Closes #

## 主要改动

<!-- 列出主要实现、文档或配置改动。 -->

## 测试

<!-- 勾选已运行的检查；未运行时请说明原因。 -->

- [ ] `git diff --check`
- [ ] `./scripts/pre-pr-check.sh`
- [ ] `./scripts/check.sh`
- [ ] `./scripts/validate-backend.sh`
- [ ] `./scripts/validate-web.sh`
- [ ] `./scripts/validate-admin.sh`
- [ ] `./scripts/validate-docker.sh`
- [ ] 其他：

## AI 协作记录

<!-- 如使用 AI coding agent，请说明它负责的部分、人工复核点，以及未采纳/未验证的建议。未使用可写“无”。 -->

- Agent：
- 人工复核：
- 审查清单：`docs/AI审查清单.md`
- 未验证项：

## 风险

<!-- 说明行为、数据、兼容性、部署、安全或回滚风险。 -->

- 风险等级：
- 风险提示处理：<!-- 粘贴或概述 `./scripts/pre-pr-check.sh` 的 `Risk review hints`，并说明已处理/不适用原因。 -->
- 回滚方案：

## 截图 / 演示

<!-- 涉及 UI 时请提供截图或演示；不适用时写“无”。 -->

## Checklist

- [ ] PR 默认合入 `develop`，除非这是 release 或 hotfix。
- [ ] 改动范围聚焦，且与 PR 标题一致。
- [ ] lint 已通过，或已说明未运行原因。
- [ ] typecheck 已通过，或已说明未运行原因。
- [ ] test 已通过，或已说明测试缺口。
- [ ] build 已通过，或已说明未运行原因。
- [ ] 未提交密钥、token、密码、私钥或生产专用配置。
- [ ] 已运行 `./scripts/pre-pr-check.sh`，并处理或解释 `Risk review hints`。
- [ ] 如涉及 API、鉴权、数据写入、WebSocket 或部署，已说明相关风险。
- [ ] 如涉及 UI，已提供截图或演示说明。
- [ ] 如使用 AI agent，已复核其改动并记录验证结果。
