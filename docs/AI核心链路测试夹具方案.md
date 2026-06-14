# AI 核心链路测试夹具方案

本方案用于后续单独 PR 引入 AI 核心链路的测试夹具和回归测试。当前文件只定义实施路径，
不修改 `AiChatWebSocketHandler`、`StreamingPipelineOrchestrator`、provider wiring 或任何
生产配置。

关联 ADR：`docs/adr/0004-core-ai-pipeline-test-fixtures.md`

## 当前状态

- 语音链路核心路径是 STT -> LLM -> TTS。
- WebSocket 入口是 `vocata-server/src/main/java/com/vocata/ai/websocket/AiChatWebSocketHandler.java`。
- 管线编排在 `vocata-server/src/main/java/com/vocata/ai/pipeline/StreamingPipelineOrchestrator.java`。
- Provider 抽象是 `LlmProvider`、`SttClient`、`TtsClient`。
- 已有 `AiChatWebSocketHandlerTest` 覆盖 audio start/end/cancel、连接关闭、传输错误、重复
  audio_start、二进制音频帧转发和 STT event 字段归一化。
- 当前缺少可复用 fake provider、pipeline event assertion 和完整 STT -> LLM -> TTS 契约测试。

## 目标

- 让后端 AI streaming 测试不依赖真实 provider、真实密钥或网络。
- 为 WebSocket、provider、pipeline 和 sentence chunking 建立共享测试语言。
- 优先保护 barge-in、取消、错误、清理和部分失败路径。
- 给后续 AI agent 改动核心链路时提供可运行的回归网。

## 非目标

- 不改变生产 provider 选择。
- 不新增或升级依赖，除非后续 PR 单独说明必要性和替代方案。
- 不把 fake provider 注册到生产 Spring context。
- 不修改数据库 schema、迁移或业务表结构。
- 不引入浏览器 E2E 语音测试；先稳定后端契约测试。

## 推荐测试结构

后续 PR 建议只在测试目录新增或调整：

```text
vocata-server/src/test/java/com/vocata/ai/
├── pipeline/
│   ├── SentenceChunkerTest.java
│   ├── StreamingPipelineOrchestratorTextTest.java
│   └── StreamingPipelineOrchestratorVoiceTest.java
├── testing/
│   ├── AiTestFixtures.java
│   ├── FakeLlmProvider.java
│   ├── FakeSttClient.java
│   ├── FakeTtsClient.java
│   ├── PipelineEventAssert.java
│   └── MockWebSocketSessionSupport.java
└── websocket/
    └── AiChatWebSocketHandlerTest.java
```

说明：

- `FakeLlmProvider`：用脚本化 `Flux<UnifiedAiStreamChunk>` 返回 chunk、final chunk、错误和空响应。
- `FakeSttClient`：用脚本化 `Flux<SttClient.SttResult>` 返回 interim、final、空文本和 metadata
  error。
- `FakeTtsClient`：把句子映射为稳定 byte array，可指定某一句失败。
- `PipelineEventAssert`：按 type 和关键字段断言事件，不依赖 timestamp。
- `MockWebSocketSessionSupport`：复用 session、URI、userId、outbound frame 捕获和私有 session
  state 安装逻辑，减少 handler 测试重复反射代码。

## Phase 1: Test Fixtures

Outcome:

- 建立 fake provider 和事件断言工具。
- 所有 fixture 都在 `src/test/java` 下，不能被生产代码引用。

Touchpoints:

- `vocata-server/src/test/java/com/vocata/ai/testing/FakeLlmProvider.java`
- `vocata-server/src/test/java/com/vocata/ai/testing/FakeSttClient.java`
- `vocata-server/src/test/java/com/vocata/ai/testing/FakeTtsClient.java`
- `vocata-server/src/test/java/com/vocata/ai/testing/PipelineEventAssert.java`

Acceptance checks:

- fake provider 支持成功、空响应、异常和部分失败脚本。
- fake provider 不读取环境变量。
- fake provider 不发起 HTTP、WebSocket 或 SDK 调用。
- fixture 类没有 `@Component`、`@Service` 或生产 profile 配置。

## Phase 2: Sentence Chunking Contract

Outcome:

- 用单元测试固定 `SentenceChunker` 的切句规则。

Touchpoints:

- `vocata-server/src/test/java/com/vocata/ai/pipeline/SentenceChunkerTest.java`

Acceptance checks:

- 句末标点立即切句。
- 逗号类软分隔在超过阈值后切句。
- 超长无标点文本会强制切句。
- 流结束时输出剩余文本。
- 空 chunk 和 null content 不产生句子。

## Phase 3: Text Pipeline Contract

Outcome:

- 覆盖文本消息的 LLM -> SentenceChunker -> TTS -> event flow。

Touchpoints:

- `vocata-server/src/test/java/com/vocata/ai/pipeline/StreamingPipelineOrchestratorTextTest.java`
- `vocata-server/src/test/java/com/vocata/ai/testing/AiTestFixtures.java`

Acceptance checks:

- 文本消息先进入 `PROCESSING` / `SPEAKING` 可观察状态，再最终回到 `IDLE`。
- LLM chunk 以 `llm_text_stream` 事件交付。
- 完整句子触发 `sentence_audio`，且 `sentenceIndex` 稳定递增。
- 用户消息和角色回复持久化 collaborator 被调用。
- TTS 某一句失败时，已经交付的 LLM 文本仍可保存，且测试明确记录当前预期行为。

## Phase 4: Voice Pipeline Contract

Outcome:

- 覆盖语音消息的 STT -> LLM -> TTS 关键分支。

Touchpoints:

- `vocata-server/src/test/java/com/vocata/ai/pipeline/StreamingPipelineOrchestratorVoiceTest.java`

Acceptance checks:

- interim STT 和 final STT 都能形成 `stt_result` 事件。
- 只有非错误 final STT 会进入 LLM。
- STT metadata 带 `error` 时，不调用 LLM/TTS。
- 空文本 STT 不进入 LLM。
- audio stream complete 后管线完成，不泄漏 active sink。

## Phase 5: WebSocket Regression Helpers

Outcome:

- 现有 `AiChatWebSocketHandlerTest` 保持覆盖范围，并减少重复 mock/反射逻辑。

Touchpoints:

- `vocata-server/src/test/java/com/vocata/ai/testing/MockWebSocketSessionSupport.java`
- `vocata-server/src/test/java/com/vocata/ai/websocket/AiChatWebSocketHandlerTest.java`

Acceptance checks:

- 现有 handler 测试语义不减少。
- 新 helper 能捕获 outbound text/binary frame。
- duplicate `audio_start`、`audio_cancel`、transport error、connection close 仍有测试覆盖。
- 如新增 barge-in 测试，必须证明 orchestrator `bargeIn()` 返回 ack 时客户端收到
  `barge_in_ack`。

## Phase 6: Validation And Rollout

Outcome:

- 后端验证脚本成为 AI 核心链路改动的默认 gate。

必跑命令：

```bash
./scripts/validate-backend.sh
```

建议补充定向命令：

```bash
cd vocata-server
mvn test -Dtest='SentenceChunkerTest,StreamingPipelineOrchestratorTextTest,StreamingPipelineOrchestratorVoiceTest,AiChatWebSocketHandlerTest'
```

Acceptance checks:

- 后端验证通过。
- 定向测试不依赖真实 provider key。
- secret scan 没有发现真实密钥。
- PR 总结写明未覆盖的手工语音 smoke 或浏览器 E2E 风险。

## 风险与缓解

| 风险 | 缓解 |
| --- | --- |
| fake provider 和真实 provider 行为漂移 | fixture 只固定 provider 抽象契约，真实 provider HTTP 契约另用 mock WebClient 测试 |
| Reactor 异步测试不稳定 | 使用有限 Flux、明确 timeout，避免 `Flux.never()` 出现在需要完成的契约测试中 |
| 测试为了通过而改变生产链路 | fixture PR 默认只改 `src/test/java`；生产代码变更需单独说明和审批 |
| 事件断言依赖 timestamp | assertion helper 忽略 timestamp，只断言 type 和业务字段 |
| provider key 泄露 | 测试数据只用 fake 字符串和 byte array，不读取或打印环境变量 |

## 验证矩阵

| 项目 | 验证 |
| --- | --- |
| Fixture 编译 | `./scripts/validate-backend.sh` |
| Sentence chunking | `SentenceChunkerTest` |
| 文本链路 | `StreamingPipelineOrchestratorTextTest` |
| 语音链路 | `StreamingPipelineOrchestratorVoiceTest` |
| WebSocket session | `AiChatWebSocketHandlerTest` |
| 密钥安全 | gitleaks / regex secret scan |

## 下一步执行清单

1. 确认维护者接受 `docs/adr/0004-core-ai-pipeline-test-fixtures.md`。
2. 在独立 PR 中新增 `com.vocata.ai.testing` 测试夹具。
3. 先补 `SentenceChunkerTest`，再补 pipeline 契约测试。
4. 提取 WebSocket session helper，保持现有测试语义不减少。
5. 运行 `./scripts/validate-backend.sh`，在 PR 总结中记录真实结果和剩余风险。
