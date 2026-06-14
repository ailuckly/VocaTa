# ADR 0004: Add Core AI Pipeline Test Fixtures

Date: 2026-06-13

Status: Proposed

## Context

VocaTa's most fragile runtime path is the real-time AI conversation chain:

- `AiChatWebSocketHandler` owns WebSocket session state, audio control messages, and client
  frame mapping.
- `StreamingPipelineOrchestrator` coordinates text and voice flows through
  STT -> LLM -> sentence chunking -> TTS.
- `LlmProvider`, `SttClient`, and `TtsClient` have multiple third-party implementations that
  require provider credentials, network access, and provider-specific timing behavior.

The repository already has useful WebSocket session tests in
`vocata-server/src/test/java/com/vocata/ai/websocket/AiChatWebSocketHandlerTest.java`, covering
audio start/end/cancel, duplicate audio sessions, cleanup, binary frames, and STT event
normalization. The remaining gap is a reusable, deterministic test fixture layer for provider
and pipeline contracts.

Without that layer, every future change to streaming behavior risks either:

- depending on real provider calls in tests, which is slow, flaky, and unsafe for secrets; or
- using one-off mocks that do not preserve shared protocol expectations across STT, LLM, TTS,
  pipeline, and WebSocket tests.

The AI pipeline and provider wiring are high-risk areas in `AGENTS.md`. This ADR records the
desired testing direction, but does not approve changing production provider wiring or core
runtime behavior in the current documentation PR.

## Decision

Introduce test-only AI pipeline fixtures in a dedicated follow-up PR.

The fixture layer should live under `vocata-server/src/test/java/com/vocata/ai/testing/` and
should include:

- a scripted `FakeLlmProvider` implementing `LlmProvider`
- a scripted `FakeSttClient` implementing `SttClient`
- a scripted `FakeTtsClient` implementing `TtsClient`
- helpers for deterministic `PipelineEvent` assertions
- reusable WebSocket session helpers extracted from the existing handler tests where useful

The first implementation should prove these contracts:

- text flow emits `pipeline_state`, LLM text chunks, sentence audio, and `complete` in a
  deterministic shape
- voice flow forwards interim/final STT results and sends only valid final STT text to LLM
- STT error metadata is exposed to the client but does not trigger LLM/TTS
- TTS failure for one sentence does not erase already delivered LLM text
- barge-in/cancel/connection cleanup disposes active streams without leaking session state
- no regular backend test calls a real LLM/STT/TTS provider or requires provider keys

The follow-up PR must keep the fixtures test-scoped. It must not register fake providers as
production Spring beans, change default provider selection, or alter provider configuration
without a separate approved design.

## Consequences

Benefits:

- Core streaming behavior becomes reviewable through deterministic backend tests.
- Future provider and WebSocket changes get a stable regression harness.
- Local and CI validation can run without provider credentials or real network calls.
- AI agents get a safer way to modify fragile streaming code without relying on manual smoke
  testing alone.

Trade-offs:

- Fixtures can become a second protocol if they are too abstract or drift from real provider
  responses.
- The first pipeline tests will need careful construction around persistence collaborators such
  as `ConversationService`, `MessageMapper`, and `CharacterMapper`.
- Asynchronous Reactor flows may need bounded timeouts and controlled scheduling to avoid flaky
  tests.

Rollback path:

- Keep the fixture PR isolated to test sources and documentation.
- If the fixture design proves brittle, revert the fixture PR without changing production code.
- Preserve this ADR and update it with lessons learned before attempting another fixture design.

## Alternatives Considered

- Option: Continue one-off Mockito stubs in each test.
  - Why it was not chosen: this duplicates protocol assumptions and makes pipeline regressions
    harder to diagnose.
- Option: Use real sandbox provider accounts in CI.
  - Why it was not chosen: it requires secrets, network availability, provider stability, and
    may introduce cost or rate-limit failures.
- Option: Add fake providers to production configuration behind a profile.
  - Why it was not chosen: it increases provider wiring risk and is unnecessary for the first
    deterministic test layer.
- Option: Start with browser E2E voice tests.
  - Why it was not chosen: E2E is valuable later, but the backend protocol contracts should be
    stable first.

## Validation

The follow-up implementation PR should prove:

- `./scripts/validate-backend.sh` passes.
- New tests fail if fake provider outputs, pipeline events, or WebSocket cleanup behavior regress.
- Test execution does not require `QINIU_*`, `OPENAI_*`, `GEMINI_*`, `SILICONFLOW_*`,
  `XUNFEI_*`, or `VOLCAN_TTS_*` secrets.
- No files under `src/main/java` or `src/main/resources` are changed unless the PR explicitly
  requests and receives approval for core AI behavior changes.
- Secret scanning does not find provider keys, tokens, private keys, or production-only config.

## Follow-Up Plan

Implementation details are tracked in `docs/AI核心链路测试夹具方案.md`.
