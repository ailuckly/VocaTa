package com.vocata.ai.stt.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocata.ai.stt.SttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 科大讯飞WebSocket语音听写STT客户端
 * 基于科大讯飞语音听写WebSocket API实现实时语音识别
 * 文档: https://www.xfyun.cn/doc/asr/voicedictation/API.html
 */
@Service
public class XunfeiWebSocketSttClient implements SttClient {

    private static final Logger logger = LoggerFactory.getLogger(XunfeiWebSocketSttClient.class);

    @Value("${xunfei.stt.app-id:}")
    private String appId;

    @Value("${xunfei.stt.api-key:}")
    private String apiKey;

    @Value("${xunfei.stt.secret-key:}")
    private String secretKey;

    @Value("${xunfei.stt.host:iat-api.xfyun.cn}")
    private String host;

    @Value("${xunfei.stt.path:/v2/iat}")
    private String path;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderName() {
        return "科大讯飞WebSocket STT";
    }

    @Override
    public boolean isAvailable() {
        boolean isConfigured = StringUtils.hasText(appId) && !appId.equals("your-xunfei-app-id") &&
                              StringUtils.hasText(apiKey) && !apiKey.equals("your-xunfei-api-key") &&
                              StringUtils.hasText(secretKey) && !secretKey.equals("your-xunfei-secret-key");

        if (!isConfigured) {
            logger.warn("科大讯飞WebSocket STT配置不完整 - 需要配置appId、apiKey和secretKey");
        }

        return isConfigured;
    }

    @Override
    public Flux<SttResult> streamRecognize(Flux<byte[]> audioStream, SttConfig config) {
        if (!isAvailable()) {
            return Flux.error(new RuntimeException("科大讯飞WebSocket STT服务配置不完整"));
        }

        logger.info("🎤【科大讯飞WebSocket STT】开始实时语音识别，语言: {}", config.getLanguage());

        return Flux.create(sink -> {
            try {
                String wsUrl = buildWebSocketUrl();
                logger.debug("🔗 WebSocket连接地址: {}", wsUrl);

                HttpClient client = HttpClient.newHttpClient();
                WebSocket.Builder wsBuilder = client.newWebSocketBuilder();

                AtomicBoolean isConnected = new AtomicBoolean(false);
                AtomicBoolean isFirstFrame = new AtomicBoolean(true);
                AtomicInteger status = new AtomicInteger(0); // 0: 第一帧, 1: 中间帧, 2: 最后一帧

                // wpgs 模式下按 sn 累积识别段落
                ConcurrentSkipListMap<Integer, String> segments = new ConcurrentSkipListMap<>();

                // 添加心跳检测机制
                AtomicBoolean heartbeatActive = new AtomicBoolean(true);

                WebSocket webSocket = wsBuilder.buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        logger.info("🎤【科大讯飞WebSocket STT】WebSocket连接已建立");
                        isConnected.set(true);

                        // 启动心跳检测
                        startHeartbeat(webSocket, heartbeatActive);

                        WebSocket.Listener.super.onOpen(webSocket);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        try {
                            String responseText = data.toString();
                            logger.debug("🎤【科大讯飞WebSocket STT】收到响应: {}", responseText);

                            JsonNode response = objectMapper.readTree(responseText);
                            int code = response.path("code").asInt(-1);
                            if (code != 0) {
                                String msg = response.path("message").asText("未知错误");
                                logger.error("🎤【科大讯飞WebSocket STT】API错误 code={}: {}", code, msg);
                                SttResult errorResult = new SttResult();
                                errorResult.setText("API错误: " + msg);
                                errorResult.setConfidence(0.0);
                                errorResult.setFinal(true);
                                Map<String, Object> errMeta = new HashMap<>();
                                errMeta.put("error", msg);
                                errMeta.put("provider", "XunfeiWebSocketSTT");
                                errorResult.setMetadata(errMeta);
                                sink.next(errorResult);
                                return WebSocket.Listener.super.onText(webSocket, data, last);
                            }

                            JsonNode dataNode = response.path("data");
                            int frameStatus = dataNode.path("status").asInt(-1);
                            JsonNode result = dataNode.path("result");

                            // 提取本帧文字
                            StringBuilder segText = new StringBuilder();
                            JsonNode ws = result.path("ws");
                            if (ws.isArray()) {
                                for (JsonNode wsItem : ws) {
                                    for (JsonNode cwItem : wsItem.path("cw")) {
                                        String w = cwItem.path("w").asText();
                                        if (StringUtils.hasText(w)) segText.append(w);
                                    }
                                }
                            }

                            // 按 sn + pgs/rg 累积文字（wpgs 模式）
                            int sn = result.path("sn").asInt(0);
                            String pgs = result.path("pgs").asText("apd");
                            if ("rpl".equals(pgs)) {
                                JsonNode rg = result.path("rg");
                                if (rg.isArray() && rg.size() == 2) {
                                    int from = rg.get(0).asInt();
                                    int to   = rg.get(1).asInt();
                                    for (int i = from; i <= to; i++) segments.remove(i);
                                }
                            }
                            if (segText.length() > 0) {
                                segments.put(sn, segText.toString());
                            }

                            String fullText = String.join("", segments.values());
                            if (!fullText.isEmpty()) {
                                boolean isFinal = (frameStatus == 2);
                                logger.info("【科大讯飞WebSocket STT识别】文字: '{}', 最终: {}", fullText, isFinal);
                                SttResult sttResult = new SttResult();
                                sttResult.setText(fullText);
                                sttResult.setConfidence(0.95);
                                sttResult.setFinal(isFinal);
                                Map<String, Object> metadata = new HashMap<>();
                                metadata.put("provider", "XunfeiWebSocketSTT");
                                metadata.put("language", config.getLanguage());
                                metadata.put("status", frameStatus);
                                sttResult.setMetadata(metadata);
                                sink.next(sttResult);
                            }

                        } catch (Exception e) {
                            logger.error("🎤【科大讯飞WebSocket STT】解析响应失败", e);
                            SttResult errorResult = new SttResult();
                            errorResult.setText("解析响应失败: " + e.getMessage());
                            errorResult.setConfidence(0.0);
                            errorResult.setFinal(true);
                            sink.next(errorResult);
                        }

                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        logger.info("🎤【科大讯飞WebSocket STT】WebSocket连接已关闭: {} - {}", statusCode, reason);
                        heartbeatActive.set(false); // 停止心跳
                        sink.complete();
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        logger.error("🎤【科大讯飞WebSocket STT】WebSocket连接错误", error);
                        heartbeatActive.set(false); // 停止心跳
                        sink.error(error);
                        WebSocket.Listener.super.onError(webSocket, error);
                    }
                }).join();

                // 订阅音频流
                audioStream.subscribe(
                    audioData -> {
                        try {
                            if (isConnected.get() && audioData != null && audioData.length > 0) {
                                // 构建音频数据帧
                                Map<String, Object> frame = buildAudioFrame(audioData, config, status.get());
                                String frameJson = objectMapper.writeValueAsString(frame);

                                logger.debug("🎤【科大讯飞WebSocket STT】发送音频帧，状态: {}, 数据长度: {}",
                                           status.get(), audioData.length);

                                // 发送音频数据
                                webSocket.sendText(frameJson, true);

                                // 更新状态
                                if (isFirstFrame.get()) {
                                    isFirstFrame.set(false);
                                    status.set(1); // 后续为中间帧
                                }
                            }
                        } catch (Exception e) {
                            logger.error("🎤【科大讯飞WebSocket STT】发送音频数据失败", e);
                            sink.error(e);
                        }
                    },
                    error -> {
                        logger.error("🎤【科大讯飞WebSocket STT】音频流错误", error);
                        sink.error(error);
                        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Audio stream error");
                    },
                    () -> {
                        try {
                            // 发送结束帧
                            Map<String, Object> endFrame = buildAudioFrame(new byte[0], config, 2);
                            String endFrameJson = objectMapper.writeValueAsString(endFrame);

                            logger.info("【科大讯飞WebSocket STT】发送结束帧");
                            webSocket.sendText(endFrameJson, true);

                            // 使用 CompletableFuture 延迟关闭，避免阻塞 Reactor 线程
                            java.util.concurrent.CompletableFuture.delayedExecutor(
                                    1, java.util.concurrent.TimeUnit.SECONDS)
                                .execute(() -> {
                                    try {
                                        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Audio stream completed");
                                    } catch (Exception ex) {
                                        logger.debug("关闭WebSocket连接时出错（可能已关闭）", ex);
                                    }
                                });
                        } catch (Exception e) {
                            logger.error("【科大讯飞WebSocket STT】发送结束帧失败", e);
                            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "End frame error");
                        }
                    }
                );

            } catch (Exception e) {
                logger.error("🎤【科大讯飞WebSocket STT】初始化WebSocket连接失败", e);
                sink.error(e);
            }
        });
    }

    @Override
    public Mono<SttResult> recognize(byte[] audioData, SttConfig config) {
        // 将单次识别转换为流式识别
        return streamRecognize(Flux.just(audioData), config)
                .reduce("", (acc, result) -> acc + result.getText())
                .map(finalText -> {
                    SttResult result = new SttResult();
                    result.setText(finalText);
                    result.setConfidence(0.95);
                    result.setFinal(true);

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("provider", "XunfeiWebSocketSTT");
                    metadata.put("language", config.getLanguage());
                    result.setMetadata(metadata);

                    return result;
                });
    }

    /**
     * 构建WebSocket连接URL（带认证）
     * 修正版本：严格按照科大讯飞WebSocket API文档进行认证
     */
    private String buildWebSocketUrl() throws Exception {
        // 生成RFC1123格式的时间戳
        String date = ZonedDateTime.now(ZoneId.of("GMT")).format(DateTimeFormatter.RFC_1123_DATE_TIME);

        logger.debug("🔐 生成时间戳: {}", date);

        // 构建签名原文 - 严格按照文档格式
        String signatureOrigin = "host: " + host + "\n" +
                               "date: " + date + "\n" +
                               "GET " + path + " HTTP/1.1";

        logger.debug("🔐 签名原文:\n{}", signatureOrigin);

        // 进行HMAC-SHA256加密
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(hexDigits);

        logger.debug("🔐 生成签名: {}", signature);

        // 构建authorization字符串 - 修正格式，移除多余的引号
        String authorization = String.format(
            "api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"",
            apiKey, signature);

        logger.debug("🔐 Authorization字符串: {}", authorization);

        // 先 Base64 编码 authorization，再 URL 编码
        String base64Authorization = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));
        String encodedAuthorization = URLEncoder.encode(base64Authorization, StandardCharsets.UTF_8);
        String encodedDate = URLEncoder.encode(date, StandardCharsets.UTF_8);
        String encodedHost = URLEncoder.encode(host, StandardCharsets.UTF_8);

        String wsUrl = String.format("wss://%s%s?authorization=%s&date=%s&host=%s",
                           host, path, encodedAuthorization, encodedDate, encodedHost);

        logger.debug("🔐 最终WebSocket URL长度: {}", wsUrl.length());

        return wsUrl;
    }

    /**
     * 构建音频数据帧
     */
    private Map<String, Object> buildAudioFrame(byte[] audioData, SttConfig config, int status) {
        Map<String, Object> frame = new HashMap<>();

        // 通用参数
        Map<String, Object> common = new HashMap<>();
        common.put("app_id", appId);
        frame.put("common", common);

        // 业务参数 (仅在第一帧发送)
        if (status == 0) {
            Map<String, Object> business = new HashMap<>();
            business.put("language", mapLanguage(config.getLanguage()));
            business.put("domain", "iat");
            business.put("accent", "mandarin");
            business.put("vad_eos", 3000);
            business.put("dwa", "wpgs");
            business.put("ptt", 1);
            business.put("nunum", 0);
            frame.put("business", business);
        }

        // 数据参数
        Map<String, Object> data = new HashMap<>();
        data.put("status", status); // 0:第一帧, 1:中间帧, 2:最后一帧
        data.put("format", "audio/L16;rate=16000"); // 音频格式
        data.put("encoding", "raw");

        if (audioData.length > 0) {
            String base64Audio = Base64.getEncoder().encodeToString(audioData);
            data.put("audio", base64Audio);
        }

        frame.put("data", data);

        return frame;
    }

    /**
     * 映射语言代码
     */
    private String mapLanguage(String language) {
        if (language == null) return "zh_cn";

        switch (language.toLowerCase()) {
            case "zh-cn":
            case "zh_cn":
            case "chinese":
                return "zh_cn";
            case "en-us":
            case "en_us":
            case "english":
                return "en_us";
            default:
                return "zh_cn";
        }
    }

    /**
     * 解析WebSocket响应
     */
    private SttResult parseWebSocketResponse(JsonNode response, SttConfig config) {
        try {
            int code = response.path("code").asInt();
            if (code != 0) {
                String message = response.path("message").asText();
                logger.error("🎤【科大讯飞WebSocket STT】API错误: {} - {}", code, message);

                SttResult errorResult = new SttResult();
                errorResult.setText("API错误: " + message);
                errorResult.setConfidence(0.0);
                errorResult.setFinal(true);
                return errorResult;
            }

            JsonNode data = response.path("data");
            if (data.isMissingNode()) {
                return null;
            }

            JsonNode result = data.path("result");
            if (result.isMissingNode()) {
                return null;
            }

            JsonNode ws = result.path("ws");
            if (ws.isMissingNode() || !ws.isArray()) {
                return null;
            }

            // 解析识别结果
            StringBuilder text = new StringBuilder();
            for (JsonNode wsItem : ws) {
                JsonNode cw = wsItem.path("cw");
                if (cw.isArray()) {
                    for (JsonNode cwItem : cw) {
                        String word = cwItem.path("w").asText();
                        if (StringUtils.hasText(word)) {
                            text.append(word);
                        }
                    }
                }
            }

            if (text.length() == 0) {
                return null;
            }

            SttResult sttResult = new SttResult();
            sttResult.setText(text.toString());
            sttResult.setConfidence(0.95); // 科大讯飞不直接提供置信度
            sttResult.setFinal(data.path("status").asInt() == 2); // 2表示最终结果

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("provider", "XunfeiWebSocketSTT");
            metadata.put("language", config.getLanguage());
            metadata.put("status", data.path("status").asInt());
            sttResult.setMetadata(metadata);

            return sttResult;

        } catch (Exception e) {
            logger.error("🎤【科大讯飞WebSocket STT】解析响应失败", e);
            SttResult errorResult = new SttResult();
            errorResult.setText("解析响应失败: " + e.getMessage());
            errorResult.setConfidence(0.0);
            errorResult.setFinal(true);
            return errorResult;
        }
    }

    /**
     * 启动心跳检测机制
     */
    private void startHeartbeat(WebSocket webSocket, AtomicBoolean heartbeatActive) {
        Thread heartbeatThread = new Thread(() -> {
            try {
                while (heartbeatActive.get() && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(30000); // 每30秒发送一次心跳

                    if (heartbeatActive.get() && webSocket.isOutputClosed() == false) {
                        // 发送心跳帧（空的音频帧）
                        try {
                            Map<String, Object> heartbeatFrame = new HashMap<>();
                            Map<String, Object> common = new HashMap<>();
                            common.put("app_id", appId);
                            heartbeatFrame.put("common", common);

                            Map<String, Object> data = new HashMap<>();
                            data.put("status", 1); // 中间帧
                            data.put("format", "audio/L16;rate=16000");
                            data.put("encoding", "raw");
                            data.put("audio", ""); // 空音频作为心跳
                            heartbeatFrame.put("data", data);

                            String heartbeatJson = objectMapper.writeValueAsString(heartbeatFrame);
                            webSocket.sendText(heartbeatJson, true);

                            logger.debug("🎤【科大讯飞WebSocket STT】发送心跳包");
                        } catch (Exception e) {
                            logger.warn("🎤【科大讯飞WebSocket STT】心跳发送失败", e);
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.info("🎤【科大讯飞WebSocket STT】心跳线程被中断");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("🎤【科大讯飞WebSocket STT】心跳线程异常", e);
            }
        });

        heartbeatThread.setName("XunfeiSTT-Heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }
}