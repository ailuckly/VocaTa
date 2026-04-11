package com.vocata.ai.websocket;

import cn.dev33.satoken.stp.StpUtil;
import com.vocata.ai.llm.LlmProvider;
import com.vocata.ai.pipeline.PipelineEvent;
import com.vocata.ai.pipeline.PipelineState;
import com.vocata.ai.pipeline.StreamingPipelineOrchestrator;
import com.vocata.ai.service.AiPromptEnhanceService;
import com.vocata.ai.service.AiStreamingService;
import com.vocata.ai.stt.SttClient;
import com.vocata.ai.tts.TtsClient;
import com.vocata.character.mapper.CharacterMapper;
import com.vocata.character.service.CharacterChatCountService;
import com.vocata.conversation.mapper.ConversationMapper;
import com.vocata.conversation.mapper.MessageMapper;
import com.vocata.conversation.service.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI语音对话WebSocket处理器
 * 使用 StreamingPipelineOrchestrator 编排 STT → LLM → TTS 全链路
 *
 * Client → Server:
 *   audio_start  : 开始录音，初始化音频流
 *   binary frame : 音频数据块
 *   audio_end    : 结束录音，触发处理管线
 *   audio_cancel : 取消录音
 *   text_message : 文字消息（跳过 STT）
 *   barge_in     : 客户端检测到用户插话，请求打断
 *   ping         : 心跳
 *
 * Server → Client:
 *   stt_result      : STT 中间/最终识别结果
 *   llm_text_stream : LLM token 级文本流
 *   sentence_audio  : 句子级 TTS 音频（JSON 元数据）
 *   tts_audio_meta  : 音频元数据（二进制帧之前发送）
 *   barge_in_ack    : 打断确认
 *   pipeline_state  : 管线状态变更
 *   status / error / complete / pong
 */
@Component
public class AiChatWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AiChatWebSocketHandler.class);

    // ── 注入依赖（用于构建每个会话的 Orchestrator）──
    @Autowired private LlmProvider llmProvider;
    @Autowired private SttClient sttClient;
    @Autowired private TtsClient ttsClient;
    @Autowired private ConversationService conversationService;
    @Autowired private ConversationMapper conversationMapper;
    @Autowired private MessageMapper messageMapper;
    @Autowired private CharacterMapper characterMapper;
    @Autowired private CharacterChatCountService characterChatCountService;
    @Autowired private AiPromptEnhanceService aiPromptEnhanceService;
    @Autowired private AiStreamingService aiStreamingService;

    @Value("${qiniu.ai.default-model:x-ai/grok-4-fast}")
    private String defaultLlmModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── 每个 WebSocket 会话的状态 ──
    private static final class SessionState {
        final StreamingPipelineOrchestrator orchestrator;
        volatile Sinks.Many<byte[]> audioSink;
        volatile Disposable pipelineSubscription;

        SessionState(StreamingPipelineOrchestrator orchestrator) {
            this.orchestrator = orchestrator;
        }
    }

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════════
    //  连接生命周期
    // ═══════════════════════════════════════════════════════

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket连接建立: {}", session.getId());

        String authenticatedUserId = authenticateUser(session);
        if (authenticatedUserId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("身份验证失败"));
            return;
        }

        session.getAttributes().put("authenticatedUserId", authenticatedUserId);

        // 为此会话创建专属 Orchestrator
        StreamingPipelineOrchestrator orchestrator = new StreamingPipelineOrchestrator(
                llmProvider, sttClient, ttsClient, conversationService,
                conversationMapper, messageMapper, characterMapper,
                characterChatCountService, aiPromptEnhanceService, defaultLlmModel);

        sessions.put(session.getId(), new SessionState(orchestrator));

        sendJson(session, Map.of("type", "status", "message", "WebSocket连接已建立",
                "timestamp", System.currentTimeMillis()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        logger.info("WebSocket连接关闭: {}, 状态: {}", sessionId, status);
        cleanupSession(sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket传输错误: {}", session.getId(), exception);
        cleanupSession(session.getId());
    }

    // ═══════════════════════════════════════════════════════
    //  消息分发
    // ═══════════════════════════════════════════════════════

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws IOException {
        try {
            if (message instanceof BinaryMessage) {
                handleBinaryMessage(session, (BinaryMessage) message);
            } else if (message instanceof TextMessage) {
                handleTextMessage(session, (TextMessage) message);
            }
        } catch (Exception e) {
            logger.error("处理消息失败: {}", e.getMessage(), e);
            sendJson(session, Map.of("type", "error", "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        SessionState state = sessions.get(session.getId());
        if (state != null && state.audioSink != null) {
            byte[] audioData = message.getPayload().array();
            state.audioSink.tryEmitNext(audioData);
            logger.debug("音频数据已添加到流: {} bytes", audioData.length);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) data.get("type");

            switch (type) {
                case "audio_start":
                    handleAudioStart(session);
                    break;
                case "audio_end":
                    handleAudioEnd(session);
                    break;
                case "audio_cancel":
                    handleAudioCancel(session);
                    break;
                case "text_message":
                    handleTextInput(session, data);
                    break;
                case "barge_in":
                    handleBargeIn(session);
                    break;
                case "ping":
                    sendJson(session, Map.of("type", "pong", "timestamp", System.currentTimeMillis()));
                    break;
                default:
                    logger.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            logger.error("处理文本消息失败: {}", e.getMessage(), e);
            try {
                sendJson(session, Map.of("type", "error", "error", e.getMessage(),
                        "timestamp", System.currentTimeMillis()));
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  业务处理
    // ═══════════════════════════════════════════════════════

    private void handleAudioStart(WebSocketSession session) throws IOException {
        SessionState state = sessions.get(session.getId());
        if (state == null) return;

        if (state.orchestrator.getState() == PipelineState.SPEAKING
                || state.orchestrator.getState() == PipelineState.PROCESSING) {
            // 用户在 AI 说话时开始录音 → 触发 barge-in
            handleBargeIn(session);
        }

        if (state.audioSink != null) {
            sendJson(session, Map.of("type", "error", "error", "已有进行中的音频会话",
                    "timestamp", System.currentTimeMillis()));
            return;
        }

        String conversationUuid = extractConversationUuid(session.getUri().toString());
        String userId = (String) session.getAttributes().get("authenticatedUserId");
        if (conversationUuid == null || userId == null) {
            sendJson(session, Map.of("type", "error", "error", "缺少对话或用户上下文",
                    "timestamp", System.currentTimeMillis()));
            return;
        }

        Sinks.Many<byte[]> audioSink = Sinks.many().unicast().onBackpressureBuffer();
        state.audioSink = audioSink;

        // 订阅语音管线
        state.pipelineSubscription = state.orchestrator
                .processVoiceMessage(conversationUuid, userId, audioSink.asFlux())
                .doFinally(sig -> {
                    state.audioSink = null;
                    state.pipelineSubscription = null;
                })
                .subscribe(
                        event -> dispatchEvent(session, event),
                        error -> {
                            logger.error("语音管线失败", error);
                            sendJsonSafe(session, Map.of("type", "error",
                                    "error", error.getMessage(),
                                    "timestamp", System.currentTimeMillis()));
                        }
                );

        sendJson(session, Map.of("type", "status", "message", "开始接收音频数据",
                "timestamp", System.currentTimeMillis()));
    }

    private void handleAudioEnd(WebSocketSession session) {
        SessionState state = sessions.get(session.getId());
        if (state != null && state.audioSink != null) {
            state.audioSink.tryEmitComplete();
        }
    }

    private void handleAudioCancel(WebSocketSession session) throws IOException {
        SessionState state = sessions.get(session.getId());
        if (state != null) {
            if (state.audioSink != null) {
                state.audioSink.tryEmitComplete();
                state.audioSink = null;
            }
            if (state.pipelineSubscription != null && !state.pipelineSubscription.isDisposed()) {
                state.pipelineSubscription.dispose();
                state.pipelineSubscription = null;
            }
        }
        sendJson(session, Map.of("type", "status", "message", "录音已取消",
                "timestamp", System.currentTimeMillis()));
    }

    @SuppressWarnings("unchecked")
    private void handleTextInput(WebSocketSession session, Map<String, Object> data) throws IOException {
        Map<String, Object> messageData = (Map<String, Object>) data.get("data");
        if (messageData == null) {
            sendJson(session, Map.of("type", "error", "error", "缺少data字段",
                    "timestamp", System.currentTimeMillis()));
            return;
        }

        String text = (String) messageData.get("message");
        if (text == null || text.trim().isEmpty()) {
            sendJson(session, Map.of("type", "error", "error", "文字内容不能为空",
                    "timestamp", System.currentTimeMillis()));
            return;
        }

        String conversationUuid = extractConversationUuid(session.getUri().toString());
        String userId = (String) session.getAttributes().get("authenticatedUserId");
        if (conversationUuid == null || userId == null) {
            sendJson(session, Map.of("type", "error", "error", "缺少对话或用户上下文",
                    "timestamp", System.currentTimeMillis()));
            return;
        }

        SessionState state = sessions.get(session.getId());
        if (state == null) return;

        logger.info("文字输入处理: 会话={}, 用户={}", conversationUuid, userId);

        state.pipelineSubscription = state.orchestrator
                .processTextMessage(conversationUuid, userId, text)
                .doFinally(sig -> state.pipelineSubscription = null)
                .subscribe(
                        event -> dispatchEvent(session, event),
                        error -> {
                            logger.error("文字管线失败", error);
                            sendJsonSafe(session, Map.of("type", "error",
                                    "error", error.getMessage(),
                                    "timestamp", System.currentTimeMillis()));
                        }
                );
    }

    private void handleBargeIn(WebSocketSession session) throws IOException {
        SessionState state = sessions.get(session.getId());
        if (state == null) return;

        Optional<PipelineEvent.BargeInAck> ack = state.orchestrator.bargeIn();
        if (ack.isPresent()) {
            // 取消订阅
            if (state.pipelineSubscription != null && !state.pipelineSubscription.isDisposed()) {
                state.pipelineSubscription.dispose();
                state.pipelineSubscription = null;
            }
            if (state.audioSink != null) {
                state.audioSink.tryEmitComplete();
                state.audioSink = null;
            }

            logger.info("Barge-in 执行成功，截断文本长度: {}", ack.get().getTruncatedText().length());
            dispatchEvent(session, ack.get());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  PipelineEvent → WebSocket 消息
    // ═══════════════════════════════════════════════════════

    private void dispatchEvent(WebSocketSession session, PipelineEvent event) {
        try {
            if (!session.isOpen()) return;

            if (event instanceof PipelineEvent.SttResult e) {
                sendJson(session, Map.of(
                        "type", "stt_result",
                        "text", e.getText(),
                        "isFinal", e.isFinal(),
                        "confidence", e.getConfidence(),
                        "timestamp", e.getTimestamp()
                ));

            } else if (event instanceof PipelineEvent.LlmTextChunk e) {
                sendJson(session, Map.of(
                        "type", "llm_text_stream",
                        "text", e.getText(),
                        "accumulatedText", e.getAccumulatedText() != null ? e.getAccumulatedText() : "",
                        "characterName", e.getCharacterName(),
                        "isComplete", e.isComplete(),
                        "timestamp", e.getTimestamp()
                ));

            } else if (event instanceof PipelineEvent.SentenceAudio e) {
                // 先发句子音频元数据
                sendJson(session, Map.of(
                        "type", "sentence_audio",
                        "text", e.getCorrespondingText(),
                        "sentenceIndex", e.getSentenceIndex(),
                        "audioSize", e.getAudioData().length,
                        "format", e.getAudioFormat(),
                        "sampleRate", e.getSampleRate(),
                        "timestamp", e.getTimestamp()
                ));
                // 再发二进制音频数据
                sendAudioBinary(session, e.getAudioData());

            } else if (event instanceof PipelineEvent.AudioMeta e) {
                sendJson(session, Map.of(
                        "type", "tts_audio_meta",
                        "audioSize", e.getAudioSize(),
                        "format", e.getFormat(),
                        "sampleRate", e.getSampleRate(),
                        "timestamp", e.getTimestamp()
                ));

            } else if (event instanceof PipelineEvent.BargeInAck e) {
                sendJson(session, Map.of(
                        "type", "barge_in_ack",
                        "truncatedText", e.getTruncatedText(),
                        "timestamp", e.getTimestamp()
                ));

            } else if (event instanceof PipelineEvent.StateChange e) {
                sendJson(session, Map.of(
                        "type", "pipeline_state",
                        "state", e.getState().name(),
                        "timestamp", e.getTimestamp()
                ));

            } else if (event instanceof PipelineEvent.Status e) {
                sendJson(session, Map.of(
                        "type", "status",
                        "message", e.getMessage(),
                        "timestamp", e.getTimestamp()
                ));

            } else if (event instanceof PipelineEvent.Complete) {
                sendJson(session, Map.of(
                        "type", "complete",
                        "message", "处理完成",
                        "timestamp", event.getTimestamp()
                ));

            } else if (event instanceof PipelineEvent.Error e) {
                sendJson(session, Map.of(
                        "type", "error",
                        "error", e.getError(),
                        "timestamp", e.getTimestamp()
                ));
            }
        } catch (IOException e) {
            logger.error("发送事件失败: {}", event.getType(), e);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════════════════

    private void sendJson(WebSocketSession session, Map<String, Object> data) throws IOException {
        if (!session.isOpen()) return;
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
    }

    private void sendJsonSafe(WebSocketSession session, Map<String, Object> data) {
        try {
            sendJson(session, data);
        } catch (IOException e) {
            logger.error("发送JSON失败", e);
        }
    }

    private void sendAudioBinary(WebSocketSession session, byte[] audioData) throws IOException {
        if (!session.isOpen() || audioData == null || audioData.length == 0) return;

        final int MAX_CHUNK_SIZE = 32 * 1024;
        if (audioData.length <= MAX_CHUNK_SIZE) {
            session.sendMessage(new BinaryMessage(audioData));
        } else {
            int totalChunks = (int) Math.ceil((double) audioData.length / MAX_CHUNK_SIZE);
            for (int i = 0; i < totalChunks; i++) {
                int start = i * MAX_CHUNK_SIZE;
                int end = Math.min(start + MAX_CHUNK_SIZE, audioData.length);
                byte[] chunk = java.util.Arrays.copyOfRange(audioData, start, end);
                session.sendMessage(new BinaryMessage(chunk));
            }
        }
    }

    private void cleanupSession(String sessionId) {
        SessionState state = sessions.remove(sessionId);
        if (state == null) return;

        state.orchestrator.dispose();
        if (state.audioSink != null) {
            state.audioSink.tryEmitComplete();
        }
        if (state.pipelineSubscription != null && !state.pipelineSubscription.isDisposed()) {
            state.pipelineSubscription.dispose();
        }
    }

    private String authenticateUser(WebSocketSession session) {
        try {
            String uri = session.getUri().toString();
            String token = null;

            if (uri.contains("token=")) {
                String query = uri.split("\\?")[1];
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {
                        token = param.substring("token=".length());
                        token = java.net.URLDecoder.decode(token, "UTF-8");
                        break;
                    }
                }
            }

            if (token == null) {
                token = session.getHandshakeHeaders().getFirst("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
            }

            if (token == null) {
                logger.error("WebSocket连接缺少认证token");
                return null;
            }

            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                logger.error("无效的WebSocket认证token");
                return null;
            }

            logger.info("WebSocket认证成功，用户ID: {}", loginId);
            return loginId.toString();
        } catch (Exception e) {
            logger.error("WebSocket用户认证异常", e);
            return null;
        }
    }

    private String extractConversationUuid(String uri) {
        try {
            String path = uri.split("\\?")[0];
            String[] parts = path.split("/");
            if (parts.length >= 3 && "chat".equals(parts[parts.length - 2])) {
                return parts[parts.length - 1];
            }
        } catch (Exception e) {
            logger.error("提取对话UUID失败: {}", uri, e);
        }
        return null;
    }
}
