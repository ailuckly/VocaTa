package com.vocata.ai.websocket;

import cn.dev33.satoken.stp.StpUtil;
import com.vocata.ai.service.AiStreamingService;
import com.vocata.conversation.service.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI语音对话WebSocket处理器
 * 完整实现 STT -> LLM -> TTS 处理链路
 *
 * Protocol contract:
 * Client -> server
 * audio_start: initialize the current session stream context
 * binary audio frame: append one audio chunk to current session stream
 * audio_end: complete current audio stream and trigger downstream processing
 * audio_cancel: abort current audio stream and discard partial session state
 * ping: keepalive control message
 *
 * Server -> client
 * stt_result: incremental transcript, may be interim or final
 * status: connection / call lifecycle notices
 * error: protocol or processing failure
 */
@Component
public class AiChatWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AiChatWebSocketHandler.class);

    @Autowired
    private AiStreamingService aiStreamingService;

    @Autowired
    private ConversationService conversationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 存储每个会话的音频流
    private final Map<String, Sinks.Many<byte[]>> audioSinks = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("AI语音WebSocket连接建立: {}", session.getId());

        // 验证用户身份
        String authenticatedUserId = authenticateUser(session);
        if (authenticatedUserId == null) {
            logger.error("WebSocket连接验证失败，关闭连接: {}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("身份验证失败"));
            return;
        }

        // 将认证的用户ID存储到session中
        session.getAttributes().put("authenticatedUserId", authenticatedUserId);
        logger.info("WebSocket用户认证成功: {} - 用户ID: {}", session.getId(), authenticatedUserId);

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "status",
                "message", "WebSocket连接已建立",
                "timestamp", System.currentTimeMillis()
        ))));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws IOException {
        try {
            if (message instanceof BinaryMessage) {
                handleBinaryMessage(session, (BinaryMessage) message);
            } else if (message instanceof TextMessage) {
                handleTextMessage(session, (TextMessage) message);
            }
        } catch (IOException e) {
            logger.error("处理WebSocket消息失败: {}", e.getMessage(), e);
            try {
                sendErrorMessage(session, "消息处理失败: " + e.getMessage());
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        String sessionId = session.getId();
        byte[] audioData = message.getPayload().array();

        logger.info("🎵 接收音频数据: {} bytes", audioData.length);

        // binary audio frame: append one audio chunk to current session stream
        Sinks.Many<byte[]> audioSink = audioSinks.get(sessionId);
        if (audioSink != null) {
            audioSink.tryEmitNext(audioData);
            logger.info("🎵 音频数据已添加到流: {} bytes", audioData.length);
        } else {
            logger.warn("未找到会话的音频流: {}", sessionId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            logger.debug("收到文本消息: {}", message.getPayload());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) data.get("type");
            String sessionId = session.getId();

            // Client -> server control messages are handled explicitly here.
            if ("audio_start".equals(type) || "audio_end".equals(type) || "audio_cancel".equals(type) || "ping".equals(type)) {
                logger.debug("收到控制指令: {}, 会话ID: {}", type, sessionId);
            } else {
                logger.info("解析消息类型: {}, 会话ID: {}", type, sessionId);
            }

            switch (type) {
                case "audio_start":
                    // audio_start: initialize the current session stream context
                    handleAudioStart(session);
                    break;
                case "audio_end":
                    // audio_end: complete current audio stream and trigger downstream processing
                    handleAudioEnd(session, data);
                    break;
                case "audio_cancel":
                    // audio_cancel: abort current audio stream and discard partial session state
                    handleAudioCancel(session);
                    break;
                case "text_message":
                    handleTextInput(session, data);
                    break;
                case "ping":
                    // ping: keepalive control message
                    sendPongMessage(session);
                    break;
                default:
                    logger.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            logger.error("处理文本消息失败: {}", e.getMessage(), e);
            try {
                sendErrorMessage(session, "消息处理失败: " + e.getMessage());
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }

    private void handleAudioStart(WebSocketSession session) throws IOException {
        String sessionId = session.getId();
        logger.info("开始音频录制: {}", sessionId);

        // 创建音频数据流
        Sinks.Many<byte[]> audioSink = Sinks.many().unicast().onBackpressureBuffer();
        audioSinks.put(sessionId, audioSink);

        sendStatusMessage(session, "开始接收音频数据");
    }

    private void handleAudioEnd(WebSocketSession session, Map<String, Object> data) throws IOException {
        String sessionId = session.getId();
        logger.info("结束音频录制: {}", sessionId);

        Sinks.Many<byte[]> audioSink = audioSinks.remove(sessionId);
        if (audioSink != null) {
            audioSink.tryEmitComplete();

            // 从URI中提取对话UUID
            String uri = session.getUri().toString();
            String conversationUuid = extractConversationUuid(uri);

            // 使用认证的用户ID，不信任URL参数
            String authenticatedUserId = (String) session.getAttributes().get("authenticatedUserId");

            if (conversationUuid != null && authenticatedUserId != null) {
                logger.info("🎤【完整AI处理】音频录制结束，开始STT->LLM->TTS处理 - 会话: {}, 用户: {}", 
                           conversationUuid, authenticatedUserId);

                // 完整AI处理链路: STT -> LLM -> TTS
                aiStreamingService.processVoiceMessage(conversationUuid, authenticatedUserId, audioSink.asFlux())
                        .subscribe(
                                response -> {
                                    try {
                                        String responseType = (String) response.get("type");

                                        if ("stt_result".equals(responseType)) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                                            if (payload != null) {
                                                sendSttResultFromPayload(session, payload);
                                            }
                                        } else if ("llm_chunk".equals(responseType)) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                                            if (payload != null) {
                                                String text = (String) payload.get("text");
                                                Boolean isFinal = (Boolean) payload.get("is_final");
                                                sendLlmTextStream(session, text != null ? text : "",
                                                        isFinal != null && isFinal);
                                            }
                                        } else if ("audio_chunk".equals(responseType)) {
                                            byte[] audioData = (byte[]) response.get("audio_data");
                                            if (audioData != null) {
                                                sendTtsAudioStream(session, audioData);
                                            }
                                        } else if ("tts_result".equals(responseType)) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> ttsPayload = (Map<String, Object>) response.get("tts_result");
                                            if (ttsPayload != null) {
                                                byte[] audioData = (byte[]) ttsPayload.get("audioData");
                                                String correspondingText = (String) ttsPayload.get("correspondingText");
                                                Object audioFormatObj = ttsPayload.get("audioFormat");
                                                String audioFormat = audioFormatObj instanceof String ?
                                                        (String) audioFormatObj : "mp3";
                                                Object sampleRateObj = ttsPayload.get("sampleRate");
                                                int sampleRate = sampleRateObj instanceof Number ?
                                                        ((Number) sampleRateObj).intValue() : 24000;
                                                String voiceId = ttsPayload.get("voiceId") instanceof String ?
                                                        (String) ttsPayload.get("voiceId") : null;

                                                Map<String, Object> ttsResultMessage = new HashMap<>();
                                                ttsResultMessage.put("type", "tts_result");
                                                ttsResultMessage.put("text", correspondingText != null ? correspondingText : "");
                                                ttsResultMessage.put("format", audioFormat);
                                                ttsResultMessage.put("sampleRate", sampleRate);
                                                if (voiceId != null) {
                                                    ttsResultMessage.put("voiceId", voiceId);
                                                }
                                                ttsResultMessage.put("timestamp", System.currentTimeMillis());

                                                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ttsResultMessage)));

                                                if (audioData != null && audioData.length > 0) {
                                                    sendTtsAudioStream(session, audioData);
                                                } else {
                                                    logger.warn("【TTS阶段】TTS结果缺少音频数据");
                                                }
                                            }
                                        } else if ("complete".equals(responseType)) {
                                            sendStatusMessage(session, "语音处理完成");
                                        }
                                    } catch (IOException e) {
                                        logger.error("发送响应失败", e);
                                    }
                                },
                                error -> {
                                    logger.error("处理语音消息失败", error);
                                    try {
                                        sendErrorMessage(session, "语音处理失败: " + error.getMessage());
                                    } catch (IOException e) {
                                        logger.error("发送错误消息失败", e);
                                    }
                                },
                                () -> {
                                    logger.info("语音消息处理完成: {}", sessionId);
                                    try {
                                        sendStatusMessage(session, "语音处理完成");
                                    } catch (IOException e) {
                                        logger.error("发送完成消息失败", e);
                                    }
                                }
                        );
            } else {
                sendErrorMessage(session, "无效的请求URI");
            }
        } else {
            sendErrorMessage(session, "未找到音频流");
        }
    }

    private void handleAudioCancel(WebSocketSession session) throws IOException {
        String sessionId = session.getId();
        logger.info("取消音频录制: {}", sessionId);

        Sinks.Many<byte[]> audioSink = audioSinks.remove(sessionId);
        if (audioSink != null) {
            audioSink.tryEmitComplete();
        }

        if (session.isOpen()) {
            sendStatusMessage(session, "录音已取消");
        }
    }

    /**
     * 发送STT识别结果（从payload中提取）
     */
    private void sendSttResultFromPayload(WebSocketSession session, Map<String, Object> payload) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "stt_result",
                    "text", payload.getOrDefault("text", ""),
                    "isFinal", payload.getOrDefault("is_final", false),
                    "confidence", payload.getOrDefault("confidence", 0.0),
                    "timestamp", System.currentTimeMillis()
            ))));
        } catch (IOException e) {
            logger.error("发送STT结果失败", e);
        }
    }

    /**
     * 发送LLM文本流 - 确保流式响应
     */
    private void sendLlmTextStream(WebSocketSession session, String content, boolean isComplete) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "llm_text_stream",
                    "text", content,
                    "characterName", "AI助手",
                    "isComplete", isComplete,
                    "timestamp", System.currentTimeMillis()
            ))));
        } catch (IOException e) {
            logger.error("发送LLM文本流失败", e);
        }
    }

    /**
     * 发送TTS音频流 - 使用二进制分片传输避免64KB限制
     */
    private void sendTtsAudioStream(WebSocketSession session, byte[] audioData) {
        try {
            if (!session.isOpen()) {
                logger.warn("会话已关闭，跳过发送TTS音频数据");
                return;
            }
            logger.info("【TTS输出】发送音频数据到前端 - 大小: {} bytes", audioData.length);

            // 先发送音频元数据（JSON格式）
            Map<String, Object> audioMeta = Map.of(
                    "type", "tts_audio_meta",
                    "audioSize", audioData.length,
                    "format", "mp3",
                    "sampleRate", 24000,
                    "channels", 1,
                    "bitDepth", 16,
                    "timestamp", System.currentTimeMillis()
            );
            String audioMetaJson = objectMapper.writeValueAsString(audioMeta);
            logger.info("【TTS输出】发送音频元数据: {}", audioMetaJson);
            session.sendMessage(new TextMessage(audioMetaJson));

            // 检查音频数据大小，如果超过32KB则分片传输，避免客户端因单帧过大触发协议错误
            final int MAX_CHUNK_SIZE = 32 * 1024; // 32KB每片，更好的兼容性

            if (audioData.length <= MAX_CHUNK_SIZE) {
                // 小于50KB，直接发送二进制消息
                session.sendMessage(new BinaryMessage(audioData));
                logger.info("【TTS输出】音频数据一次性发送完成 - {} bytes", audioData.length);
            } else {
                // 大于50KB，分片发送
                int totalChunks = (int) Math.ceil((double) audioData.length / MAX_CHUNK_SIZE);
                logger.info("【TTS输出】音频数据过大，分{}片发送 - 总大小: {} bytes", totalChunks, audioData.length);

                for (int i = 0; i < totalChunks; i++) {
                    int start = i * MAX_CHUNK_SIZE;
                    int end = Math.min(start + MAX_CHUNK_SIZE, audioData.length);
                    byte[] chunk = java.util.Arrays.copyOfRange(audioData, start, end);

                    // 发送分片数据
                    session.sendMessage(new BinaryMessage(chunk));
                    logger.info("【TTS输出】发送音频分片 {}/{} - {} bytes", i + 1, totalChunks, chunk.length);

                    // 分片间短暂延迟，避免网络拥塞
                    Thread.sleep(10);
                }
                logger.info("【TTS输出】音频分片发送完成 - 共{}片", totalChunks);
            }

        } catch (Exception e) {
            logger.error("【TTS输出】发送TTS音频流失败", e);
        }
    }

    /**
     * 处理文字输入消息
     * 用户可以发送文字消息，AI将返回双重响应（文字+语音）
     */
    private void handleTextInput(WebSocketSession session, Map<String, Object> data) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> messageData = (Map<String, Object>) data.get("data");

        if (messageData == null) {
            sendErrorMessage(session, "缺少data字段");
            return;
        }

        String text = (String) messageData.get("message");
        if (text == null || text.trim().isEmpty()) {
            sendErrorMessage(session, "文字内容不能为空");
            return;
        }

        // 从URI中提取对话UUID
        String uri = session.getUri().toString();
        String conversationUuidStr = extractConversationUuid(uri);

        if (conversationUuidStr == null) {
            sendErrorMessage(session, "无效的请求URI，缺少对话UUID");
            return;
        }

        // 使用认证的用户ID，不信任URL参数
        String authenticatedUserId = (String) session.getAttributes().get("authenticatedUserId");
        if (authenticatedUserId == null) {
            sendErrorMessage(session, "用户身份验证失败");
            return;
        }

        logger.info("【文字输入处理】开始处理 - 会话UUID: {}, 认证用户: {}, 文字内容: '{}'",
                conversationUuidStr, authenticatedUserId, text);

        try {
            // 完整AI模式: 文本消息 -> LLM -> TTS
            aiStreamingService.processTextMessage(conversationUuidStr, authenticatedUserId, text)
                    .subscribe(
                            response -> {
                                try {
                                    String responseType = (String) response.get("type");
                                    logger.info("【WebSocket响应】收到响应类型: {}", responseType);

                                    if ("text_chunk".equals(responseType)) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                                        if (payload != null) {
                                            String responseText = (String) payload.get("text");
                                            Boolean isFinal = (Boolean) payload.get("is_final");

                                            logger.info("【LLM阶段】流式文本响应 - 内容: '{}', 是否完整: {}",
                                                    responseText, isFinal);

                                            // 发送流式文本响应
                                            sendLlmTextStream(session, responseText != null ? responseText : "",
                                                    isFinal != null && isFinal);
                                        }

                                    } else if ("audio_chunk".equals(responseType)) {
                                        byte[] audioData = (byte[]) response.get("audio_data");
                                        if (audioData != null) {
                                            logger.info("【TTS阶段】收到音频块，大小: {} bytes", audioData.length);

                                            // 发送语音响应
                                            sendTtsAudioStream(session, audioData);
                                        }

                                    } else if ("audio_complete".equals(responseType)) {
                                        logger.info("【TTS阶段】音频合成完成");
                                        // 可以发送音频完成标志
                                        sendStatusMessage(session, "音频合成完成");

                                    } else if ("complete".equals(responseType)) {
                                        logger.info("【处理完成】文字消息处理链路完成");
                                        sendStatusMessage(session, "处理完成");

                                    } else if ("error".equals(responseType)) {
                                        String errorMessage = (String) response.get("error");
                                        if (errorMessage == null) {
                                            errorMessage = (String) response.get("message");
                                        }
                                        logger.error("【处理错误】: {}", errorMessage);
                                        sendErrorMessage(session, errorMessage != null ? errorMessage : "处理失败");
                                    }

                                } catch (Exception e) {
                                    logger.error("【响应处理错误】: {}", e.getMessage(), e);
                                    try {
                                        sendErrorMessage(session, "响应处理失败");
                                    } catch (IOException ex) {
                                        logger.error("发送错误消息失败", ex);
                                    }
                                }
                            },
                            error -> {
                                logger.error("【文字消息处理失败】: {}", error.getMessage(), error);
                                try {
                                    sendErrorMessage(session, "文字消息处理失败: " + error.getMessage());
                                } catch (IOException ex) {
                                    logger.error("发送错误消息失败", ex);
                                }
                            }
                    );

        } catch (Exception e) {
            logger.error("【参数错误】UUID或用户ID格式错误: conversationUuid={}, userId={}", 
                        conversationUuidStr, authenticatedUserId);
            sendErrorMessage(session, "参数格式错误");
        }
    }

    private void sendStatusMessage(WebSocketSession session, String message) throws IOException {
        if (!session.isOpen()) {
            logger.warn("会话已关闭，无法发送状态消息: {}", message);
            return;
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "status",
                "message", message,
                "timestamp", System.currentTimeMillis()
        ))));
    }

    private void sendErrorMessage(WebSocketSession session, String error) throws IOException {
        if (!session.isOpen()) {
            logger.warn("会话已关闭，无法发送错误消息: {}", error);
            return;
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "error",
                "error", error,
                "timestamp", System.currentTimeMillis()
        ))));
    }

    private void sendPongMessage(WebSocketSession session) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "pong",
                "timestamp", System.currentTimeMillis()
        ))));
    }

    /**
     * 验证WebSocket用户身份
     */
    private String authenticateUser(WebSocketSession session) {
        try {
            // 尝试从URL参数中获取token
            String uri = session.getUri().toString();
            logger.info("【认证调试】WebSocket URI: {}", uri);
            String token = null;

            if (uri.contains("token=")) {
                String query = uri.split("\\?")[1];
                logger.info("【认证调试】查询参数: {}", query);
                String[] params = query.split("&");
                for (String param : params) {
                    logger.info("【认证调试】处理参数: {}", param);
                    if (param.startsWith("token=")) {
                        token = param.substring("token=".length());
                        // URL解码token
                        token = java.net.URLDecoder.decode(token, "UTF-8");
                        logger.info("【认证调试】从URL参数提取token: {}...", 
                                   token.substring(0, Math.min(token.length(), 20)));
                        break;
                    }
                }
            }

            // 如果URL参数中没有token，尝试从handshake headers中获取
            if (token == null) {
                logger.info("【认证调试】URL参数中未找到token，尝试从headers获取");
                token = session.getHandshakeHeaders().getFirst("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                    logger.info("【认证调试】从Authorization header提取token: {}...", 
                               token.substring(0, Math.min(token.length(), 20)));
                }
            }

            if (token == null) {
                logger.error("【认证失败】WebSocket连接缺少认证token");
                return null;
            }

            // 使用Sa-Token验证token
            logger.info("【认证调试】开始验证token...");
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                logger.error("【认证失败】无效的WebSocket认证token: {}...", 
                            token.substring(0, Math.min(token.length(), 20)));
                return null;
            }

            logger.info("【认证成功】用户ID: {}", loginId);
            return loginId.toString();
        } catch (Exception e) {
            logger.error("【认证异常】WebSocket用户认证异常", e);
            return null;
        }
    }

    private String extractConversationUuid(String uri) {
        // 从URI中提取对话标识符: /ws/chat/{conversation_uuid}?userId=1
        try {
            String path = uri.split("\\?")[0]; // 去掉查询参数
            String[] parts = path.split("/");
            if (parts.length >= 3 && "chat".equals(parts[parts.length - 2])) {
                return parts[parts.length - 1]; // conversation_uuid
            }
        } catch (Exception e) {
            logger.error("提取对话UUID失败: {}", uri, e);
        }
        return null;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        String sessionId = session.getId();
        logger.info("AI语音WebSocket连接关闭: {}, 状态: {}", sessionId, status);

        // 清理资源
        Sinks.Many<byte[]> audioSink = audioSinks.remove(sessionId);
        if (audioSink != null) {
            audioSink.tryEmitComplete();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws IOException {
        logger.error("WebSocket传输错误: {}", session.getId(), exception);

        // 清理资源
        String sessionId = session.getId();
        Sinks.Many<byte[]> audioSink = audioSinks.remove(sessionId);
        if (audioSink != null) {
            audioSink.tryEmitComplete();
        }
    }
}
