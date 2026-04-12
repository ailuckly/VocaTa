package com.vocata.ai.pipeline;

import com.vocata.ai.dto.UnifiedAiRequest;
import com.vocata.ai.dto.UnifiedAiStreamChunk;
import com.vocata.ai.llm.LlmProvider;
import com.vocata.ai.service.AiPromptEnhanceService;
import com.vocata.ai.stt.SttClient;
import com.vocata.ai.tts.TtsClient;
import com.vocata.character.entity.Character;
import com.vocata.character.mapper.CharacterMapper;
import com.vocata.character.service.CharacterChatCountService;
import com.vocata.conversation.constants.ContentType;
import com.vocata.conversation.constants.SenderType;
import com.vocata.conversation.entity.Conversation;
import com.vocata.conversation.entity.Message;
import com.vocata.conversation.mapper.ConversationMapper;
import com.vocata.conversation.mapper.MessageMapper;
import com.vocata.conversation.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流式对话管线编排器
 *
 * 每个 WebSocket 会话持有一个实例。管理 STT→LLM→TTS 完整链路，
 * 支持 barge-in 打断和对话历史精确对齐。
 *
 * 状态机: IDLE → LISTENING → PROCESSING → SPEAKING → IDLE
 *                                              ↑
 *                                    BARGE_IN ──┘
 */
public class StreamingPipelineOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(StreamingPipelineOrchestrator.class);

    // ── 外部依赖（通过构造函数注入）──
    private final LlmProvider llmProvider;
    private final SttClient sttClient;
    private final TtsClient ttsClient;
    private final ConversationService conversationService;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final CharacterMapper characterMapper;
    private final CharacterChatCountService chatCountService;
    private final AiPromptEnhanceService promptEnhanceService;
    private final String defaultLlmModel;

    // ── 会话级状态 ──
    private final AtomicReference<PipelineState> state = new AtomicReference<>(PipelineState.IDLE);
    private volatile Disposable currentPipeline;
    private volatile Sinks.Many<byte[]> currentAudioSink;

    /** 已发送给客户端的 LLM 文本（用于 barge-in 时截断 AI 消息） */
    private final StringBuilder deliveredText = new StringBuilder();

    public StreamingPipelineOrchestrator(LlmProvider llmProvider,
                                         SttClient sttClient,
                                         TtsClient ttsClient,
                                         ConversationService conversationService,
                                         ConversationMapper conversationMapper,
                                         MessageMapper messageMapper,
                                         CharacterMapper characterMapper,
                                         CharacterChatCountService chatCountService,
                                         AiPromptEnhanceService promptEnhanceService,
                                         String defaultLlmModel) {
        this.llmProvider = llmProvider;
        this.sttClient = sttClient;
        this.ttsClient = ttsClient;
        this.conversationService = conversationService;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.characterMapper = characterMapper;
        this.chatCountService = chatCountService;
        this.promptEnhanceService = promptEnhanceService;
        this.defaultLlmModel = defaultLlmModel;
    }

    public PipelineState getState() {
        return state.get();
    }

    // ═══════════════════════════════════════════════════════
    //  文字消息处理: Text → LLM → SentenceChunker → TTS
    // ═══════════════════════════════════════════════════════

    /**
     * 处理文字消息的完整链路（跳过 STT）
     */
    public Flux<PipelineEvent> processTextMessage(String conversationUuid,
                                                   String userId,
                                                   String textMessage) {
        Long userIdLong = Long.parseLong(userId);

        return loadConversationAndCharacter(conversationUuid, userIdLong)
                .flatMapMany(ctx -> {
                    state.set(PipelineState.PROCESSING);
                    deliveredText.setLength(0);

                    return Flux.concat(
                            Flux.just(new PipelineEvent.StateChange(PipelineState.PROCESSING)),
                            buildLlmToTtsPipeline(ctx, textMessage, userIdLong),
                            Flux.just(new PipelineEvent.Complete())
                    );
                })
                .doFinally(sig -> state.set(PipelineState.IDLE))
                .onErrorResume(e -> {
                    logger.error("文字消息处理失败", e);
                    state.set(PipelineState.IDLE);
                    return Flux.just(new PipelineEvent.Error(e.getMessage()));
                });
    }

    // ═══════════════════════════════════════════════════════
    //  语音消息处理: Audio → STT → LLM → SentenceChunker → TTS
    // ═══════════════════════════════════════════════════════

    /**
     * 处理语音消息。返回的 Flux 包含 STT/LLM/TTS 所有阶段的事件。
     */
    public Flux<PipelineEvent> processVoiceMessage(String conversationUuid,
                                                    String userId,
                                                    Flux<byte[]> audioStream) {
        Long userIdLong = Long.parseLong(userId);

        return loadConversationAndCharacter(conversationUuid, userIdLong)
                .flatMapMany(ctx -> {
                    state.set(PipelineState.LISTENING);
                    deliveredText.setLength(0);

                    Character character = ctx.character;
                    SttClient.SttConfig sttConfig = new SttClient.SttConfig(character.getLanguage());

                    // STT 流
                    Flux<SttClient.SttResult> sttFlux = sttClient.streamRecognize(audioStream, sttConfig)
                            .filter(r -> r.getText() != null && !r.getText().trim().isEmpty())
                            .share(); // 允许多个订阅者

                    // STT 中间结果 → 客户端（包括错误结果，让前端知道发生了什么）
                    Flux<PipelineEvent> sttEvents = sttFlux.map(r ->
                            new PipelineEvent.SttResult(r.getText(), r.isFinal(), r.getConfidence()));

                    // 过滤 STT 错误：metadata 中带 "error" 的结果不送 LLM
                    Flux<PipelineEvent> llmTtsEvents = sttFlux
                            .filter(r -> r.isFinal() && !isSttError(r))
                            .take(1)
                            .flatMap(finalResult -> {
                                state.set(PipelineState.PROCESSING);
                                return buildLlmToTtsPipeline(ctx, finalResult.getText(), userIdLong);
                            });

                    return Flux.concat(
                            Flux.just(new PipelineEvent.StateChange(PipelineState.LISTENING)),
                            sttEvents.takeUntil(r -> ((PipelineEvent.SttResult) r).isFinal()),
                            Flux.just(new PipelineEvent.StateChange(PipelineState.PROCESSING)),
                            llmTtsEvents,
                            Flux.just(new PipelineEvent.Complete())
                    );
                })
                .doFinally(sig -> state.set(PipelineState.IDLE))
                .onErrorResume(e -> {
                    logger.error("语音消息处理失败", e);
                    state.set(PipelineState.IDLE);
                    return Flux.just(new PipelineEvent.Error(e.getMessage()));
                });
    }

    // ═══════════════════════════════════════════════════════
    //  Barge-in 打断
    // ═══════════════════════════════════════════════════════

    /**
     * 执行 barge-in 打断。取消当前 LLM+TTS 管线，截断 AI 回复。
     *
     * @return 打断事件（包含已播放的截断文本），如果当前不在 SPEAKING 状态返回 empty
     */
    public Optional<PipelineEvent.BargeInAck> bargeIn() {
        PipelineState current = state.get();
        if (current != PipelineState.SPEAKING && current != PipelineState.PROCESSING) {
            logger.debug("Barge-in 忽略：当前状态 {}", current);
            return Optional.empty();
        }

        logger.info("执行 Barge-in 打断，当前状态: {}, 已交付文本长度: {}", current, deliveredText.length());

        // 取消当前管线
        Disposable pipeline = currentPipeline;
        if (pipeline != null && !pipeline.isDisposed()) {
            pipeline.dispose();
        }

        // 完成当前音频 sink（如果有）
        Sinks.Many<byte[]> sink = currentAudioSink;
        if (sink != null) {
            sink.tryEmitComplete();
        }

        String truncated = deliveredText.toString();
        state.set(PipelineState.IDLE);

        return Optional.of(new PipelineEvent.BargeInAck(truncated));
    }

    /**
     * 清理所有资源（WebSocket 断开时调用）
     */
    public void dispose() {
        Disposable pipeline = currentPipeline;
        if (pipeline != null && !pipeline.isDisposed()) {
            pipeline.dispose();
        }
        Sinks.Many<byte[]> sink = currentAudioSink;
        if (sink != null) {
            sink.tryEmitComplete();
        }
        state.set(PipelineState.IDLE);
    }

    // ═══════════════════════════════════════════════════════
    //  内部：LLM → SentenceChunker → TTS 管线构建
    // ═══════════════════════════════════════════════════════

    private Flux<PipelineEvent> buildLlmToTtsPipeline(ConversationContext ctx,
                                                       String userText,
                                                       Long userId) {
        Conversation conversation = ctx.conversation;
        Character character = ctx.character;

        // 构建 LLM 请求
        UnifiedAiRequest llmRequest = buildLlmRequest(conversation, character, userText);

        // 句子计数器
        AtomicInteger sentenceIndex = new AtomicInteger(0);

        // 保存用户消息 → LLM 流式生成 → 句子切片 → TTS 并行合成
        return saveMessage(conversation.getId(), userText, SenderType.USER, userId)
                .thenMany(Flux.defer(() -> {
                    // LLM 流共享给: (1) 文本 chunk 事件 (2) SentenceChunker
                    Flux<UnifiedAiStreamChunk> llmFlux = llmProvider.streamChat(llmRequest).share();

                    // 文本 chunk 事件流
                    Flux<PipelineEvent> textEvents = llmFlux
                            .filter(c -> c.getContent() != null && !c.getContent().isEmpty())
                            .doOnNext(c -> deliveredText.append(c.getContent()))
                            .map(c -> new PipelineEvent.LlmTextChunk(
                                    c.getContent(),
                                    c.getAccumulatedContent(),
                                    c.getIsFinal() != null && c.getIsFinal(),
                                    character.getName()
                            ));

                    // 句子级 TTS 事件流
                    Flux<PipelineEvent> ttsEvents = SentenceChunker.chunkToSentences(llmFlux)
                            .flatMapSequential(sentence -> {
                                int idx = sentenceIndex.getAndIncrement();
                                TtsClient.TtsConfig ttsConfig = new TtsClient.TtsConfig(
                                        character.getVoiceId(), character.getLanguage());

                                return ttsClient.synthesize(sentence, ttsConfig)
                                        .map(result -> (PipelineEvent) new PipelineEvent.SentenceAudio(
                                                result.getAudioData(),
                                                sentence,
                                                idx,
                                                result.getAudioFormat() != null ? result.getAudioFormat() : "mp3",
                                                result.getSampleRate() > 0 ? result.getSampleRate() : 24000
                                        ))
                                        .onErrorResume(e -> {
                                            logger.error("TTS 合成句子 {} 失败: {}", idx, e.getMessage());
                                            return Mono.empty();
                                        });
                            }, 2); // 最多 2 个句子并行 TTS

                    // 合并：先文本 chunk（实时），然后 TTS 音频（句子级）
                    // 使用 merge 让文本和音频可以交错输出
                    return Flux.merge(textEvents, ttsEvents);
                }))
                .doOnSubscribe(sub -> state.set(PipelineState.SPEAKING))
                .concatWith(
                        // 管线完成后保存 AI 消息
                        Mono.defer(() -> {
                            String fullResponse = deliveredText.toString().trim();
                            if (fullResponse.isEmpty()) {
                                return Mono.empty();
                            }
                            return saveMessage(conversation.getId(), fullResponse, SenderType.CHARACTER, userId)
                                    .then(Mono.empty());
                        })
                );
    }

    // ═══════════════════════════════════════════════════════
    //  内部：加载对话和角色上下文
    // ═══════════════════════════════════════════════════════

    private Mono<ConversationContext> loadConversationAndCharacter(String conversationUuid, Long userId) {
        return Mono.fromCallable(() -> {
            UUID uuid = UUID.fromString(conversationUuid);
            Conversation conversation = conversationService.getConversationByUuid(uuid);
            if (conversation == null) {
                throw new RuntimeException("对话不存在: " + conversationUuid);
            }
            if (!conversation.getUserId().equals(userId)) {
                throw new RuntimeException("无权限访问此对话");
            }
            Character character = characterMapper.selectById(conversation.getCharacterId());
            if (character == null) {
                throw new RuntimeException("角色不存在: " + conversation.getCharacterId());
            }
            return new ConversationContext(conversation, character);
        });
    }

    // ═══════════════════════════════════════════════════════
    //  内部：构建 LLM 请求（复用原 AiStreamingService 逻辑）
    // ═══════════════════════════════════════════════════════

    private UnifiedAiRequest buildLlmRequest(Conversation conversation, Character character, String userText) {
        UnifiedAiRequest request = new UnifiedAiRequest();

        request.setSystemPrompt(promptEnhanceService.buildEnhancedPrompt(character));
        request.setUserMessage(userText);

        // 历史对话上下文
        List<Message> recentMessages = messageMapper.findRecentMessagesByConversationId(conversation.getId(), 20);
        Collections.reverse(recentMessages);
        List<UnifiedAiRequest.ChatMessage> contextMessages = new ArrayList<>();

        int contextWindow = character.getContextWindow() != null ? character.getContextWindow() : 10;
        int startIndex = Math.max(0, recentMessages.size() - contextWindow);

        for (int i = startIndex; i < recentMessages.size(); i++) {
            Message msg = recentMessages.get(i);
            String role = (msg.getSenderType() == SenderType.USER.getCode()) ? "user" : "assistant";
            contextMessages.add(new UnifiedAiRequest.ChatMessage(role, msg.getTextContent()));
        }

        request.setContextMessages(contextMessages);

        UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
        modelConfig.setModelName(defaultLlmModel);
        modelConfig.setTemperature(character.getTemperature() != null ?
                character.getTemperature().doubleValue() : 0.7);
        modelConfig.setContextWindow(contextWindow);
        request.setModelConfig(modelConfig);

        return request;
    }

    // ═══════════════════════════════════════════════════════
    //  内部：消息持久化
    // ═══════════════════════════════════════════════════════

    private Mono<Message> saveMessage(Long conversationId, String content, SenderType senderType, Long userId) {
        return Mono.fromCallable(() -> {
            Message message = new Message();
            message.setMessageUuid(UUID.randomUUID());
            message.setConversationId(conversationId);
            message.setSenderType(senderType.getCode());
            message.setContentType(ContentType.TEXT.getCode());
            message.setTextContent(content);
            message.setCreateId(userId);
            message.setUpdateId(userId);
            message.setCreateDate(LocalDateTime.now());
            message.setUpdateDate(LocalDateTime.now());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("processing_timestamp", LocalDateTime.now().toString());
            metadata.put("ai_provider", llmProvider.getProviderName());
            message.setMetadata(metadata);

            messageMapper.insert(message);

            if (senderType == SenderType.USER) {
                try {
                    Conversation conversation = conversationMapper.selectById(conversationId);
                    if (conversation != null && conversation.getCharacterId() != null) {
                        chatCountService.incrementChatCount(conversation.getCharacterId());
                    }
                } catch (Exception e) {
                    logger.error("增加角色聊天计数失败", e);
                }
            }

            if (senderType == SenderType.CHARACTER) {
                try {
                    conversationService.triggerTitleGenerationForNewConversation(conversationId);
                } catch (Exception e) {
                    logger.error("触发标题生成失败", e);
                }
            }

            return message;
        });
    }

    // ── 内部上下文对象 ──

    /** 检查 STT 结果是否是错误（metadata 中带 "error" 键） */
    private boolean isSttError(SttClient.SttResult result) {
        return result.getMetadata() != null && result.getMetadata().containsKey("error");
    }

    private static class ConversationContext {
        final Conversation conversation;
        final Character character;

        ConversationContext(Conversation conversation, Character character) {
            this.conversation = conversation;
            this.character = character;
        }
    }
}
