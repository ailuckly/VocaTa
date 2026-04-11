package com.vocata.ai.websocket;

import com.vocata.ai.pipeline.PipelineEvent;
import com.vocata.ai.pipeline.StreamingPipelineOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatWebSocketHandlerTest {

    private static final String SESSION_ID = "session-1";
    private static final String USER_ID = "42";

    private AiChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AiChatWebSocketHandler();
    }

    @Test
    void audioStartBeginsVoiceProcessingImmediatelyAndAudioEndOnlyCompletesStream() throws Exception {
        WebSocketSession session = mockVoiceSession();
        StreamingPipelineOrchestrator orchestrator = mock(StreamingPipelineOrchestrator.class);
        AtomicInteger voiceMessageCalls = new AtomicInteger();
        AtomicBoolean completed = new AtomicBoolean();

        when(orchestrator.processVoiceMessage(eq(conversationUuid(session)), eq(USER_ID), any()))
                .thenAnswer(invocation -> {
                    Flux<byte[]> audioStream = invocation.getArgument(2);
                    voiceMessageCalls.incrementAndGet();
                    return audioStream
                            .doOnComplete(() -> completed.set(true))
                            .thenMany(Flux.just(new PipelineEvent.Complete()));
                });

        installSessionState(session, orchestrator);

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_start\"}"));

        assertEquals(1, voiceMessageCalls.get());
        verify(orchestrator, times(1))
                .processVoiceMessage(eq(conversationUuid(session)), eq(USER_ID), any());

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_end\"}"));

        assertTrue(completed.get());
        assertTrue(sessions().containsKey(SESSION_ID));
        assertSessionAudioClosed();
    }

    @Test
    void audioCancelDisposesActiveVoiceSession() throws Exception {
        WebSocketSession session = mockVoiceSession();
        StreamingPipelineOrchestrator orchestrator = mock(StreamingPipelineOrchestrator.class);

        when(orchestrator.processVoiceMessage(eq(conversationUuid(session)), eq(USER_ID), any()))
                .thenReturn(Flux.never());

        installSessionState(session, orchestrator);

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_start\"}"));
        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_cancel\"}"));

        assertTrue(sessions().containsKey(SESSION_ID));
        assertSessionAudioClosed();
    }

    @Test
    void afterConnectionClosedDisposesActiveVoiceSession() throws Exception {
        WebSocketSession session = mockVoiceSession();
        StreamingPipelineOrchestrator orchestrator = mock(StreamingPipelineOrchestrator.class);

        when(orchestrator.processVoiceMessage(eq(conversationUuid(session)), eq(USER_ID), any()))
                .thenReturn(Flux.never());

        installSessionState(session, orchestrator);

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_start\"}"));
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(orchestrator, times(1)).dispose();
        assertFalse(sessions().containsKey(SESSION_ID));
    }

    @Test
    void transportErrorDisposesActiveVoiceSession() throws Exception {
        WebSocketSession session = mockVoiceSession();
        StreamingPipelineOrchestrator orchestrator = mock(StreamingPipelineOrchestrator.class);

        when(orchestrator.processVoiceMessage(eq(conversationUuid(session)), eq(USER_ID), any()))
                .thenReturn(Flux.never());

        installSessionState(session, orchestrator);

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_start\"}"));
        handler.handleTransportError(session, new IOException("boom"));

        verify(orchestrator, times(1)).dispose();
        assertFalse(sessions().containsKey(SESSION_ID));
    }

    @Test
    void sttResultIsForwardedToClientWithNormalizedFields() throws Exception {
        WebSocketSession session = mockVoiceSession();
        StreamingPipelineOrchestrator orchestrator = mock(StreamingPipelineOrchestrator.class);
        AtomicInteger textMessages = new AtomicInteger();
        AtomicBoolean normalizedSttSeen = new AtomicBoolean();

        doAnswer(invocation -> {
            WebSocketMessage<?> outbound = invocation.getArgument(0);
            if (outbound instanceof TextMessage textMessage) {
                textMessages.incrementAndGet();
                String payload = textMessage.getPayload();
                if (payload.contains("\"type\":\"stt_result\"")
                        && payload.contains("\"text\":\"你好\"")
                        && payload.contains("\"isFinal\":false")
                        && payload.contains("\"confidence\":0.75")) {
                    normalizedSttSeen.set(true);
                }
            }
            return null;
        }).when(session).sendMessage(any(WebSocketMessage.class));

        when(orchestrator.processVoiceMessage(eq(conversationUuid(session)), eq(USER_ID), any()))
                .thenReturn(Flux.just(
                        new PipelineEvent.SttResult("你好", false, 0.75),
                        new PipelineEvent.Complete()
                ));

        installSessionState(session, orchestrator);

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_start\"}"));
        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_end\"}"));

        verify(session, atLeastOnce()).sendMessage(any(WebSocketMessage.class));
        assertTrue(textMessages.get() >= 2);
        assertTrue(normalizedSttSeen.get());
    }

    @Test
    void duplicateAudioStartReturnsExplicitSessionError() throws Exception {
        WebSocketSession session = mockVoiceSession();
        StreamingPipelineOrchestrator orchestrator = mock(StreamingPipelineOrchestrator.class);
        AtomicInteger voiceMessageCalls = new AtomicInteger();
        AtomicBoolean duplicateStartErrorSeen = new AtomicBoolean();

        doAnswer(invocation -> {
            WebSocketMessage<?> outbound = invocation.getArgument(0);
            if (outbound instanceof TextMessage textMessage) {
                String payload = textMessage.getPayload();
                if (payload.contains("\"type\":\"error\"")
                        && payload.contains("已有进行中的音频会话")) {
                    duplicateStartErrorSeen.set(true);
                }
            }
            return null;
        }).when(session).sendMessage(any(WebSocketMessage.class));

        when(orchestrator.processVoiceMessage(eq(conversationUuid(session)), eq(USER_ID), any()))
                .thenAnswer(invocation -> {
                    voiceMessageCalls.incrementAndGet();
                    return Flux.never();
                });

        installSessionState(session, orchestrator);

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_start\"}"));
        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_start\"}"));

        assertEquals(1, voiceMessageCalls.get());
        assertTrue(duplicateStartErrorSeen.get());
    }

    @Test
    void binaryFrameIsForwardedIntoActiveAudioSink() throws Exception {
        WebSocketSession session = mockVoiceSession();
        StreamingPipelineOrchestrator orchestrator = mock(StreamingPipelineOrchestrator.class);
        AtomicInteger audioChunks = new AtomicInteger();

        when(orchestrator.processVoiceMessage(eq(conversationUuid(session)), eq(USER_ID), any()))
                .thenAnswer(invocation -> {
                    Flux<byte[]> audioStream = invocation.getArgument(2);
                    return audioStream
                            .doOnNext(chunk -> audioChunks.incrementAndGet())
                            .thenMany(Flux.never());
                });

        installSessionState(session, orchestrator);

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_start\"}"));
        handler.handleMessage(session, new BinaryMessage("abc".getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, audioChunks.get());
    }

    private WebSocketSession mockVoiceSession() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("authenticatedUserId", USER_ID);

        UUID conversationUuid = UUID.randomUUID();
        when(session.getId()).thenReturn(SESSION_ID);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/chat/" + conversationUuid));
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        doNothing().when(session).sendMessage(any(WebSocketMessage.class));

        return session;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sessions() {
        Object sessions = ReflectionTestUtils.getField(handler, "sessions");
        return sessions == null ? Map.of() : (Map<String, Object>) sessions;
    }

    private void installSessionState(WebSocketSession session, StreamingPipelineOrchestrator orchestrator) throws Exception {
        Class<?> sessionStateClass = Class.forName("com.vocata.ai.websocket.AiChatWebSocketHandler$SessionState");
        Constructor<?> constructor = sessionStateClass.getDeclaredConstructor(StreamingPipelineOrchestrator.class);
        constructor.setAccessible(true);
        Object sessionState = constructor.newInstance(orchestrator);

        @SuppressWarnings("unchecked")
        Map<String, Object> sessions = (Map<String, Object>) ReflectionTestUtils.getField(handler, "sessions");
        sessions.put(session.getId(), sessionState);
    }

    private void assertSessionAudioClosed() {
        Object sessionState = sessions().get(SESSION_ID);
        Object audioSink = ReflectionTestUtils.getField(sessionState, "audioSink");
        Object pipelineSubscription = ReflectionTestUtils.getField(sessionState, "pipelineSubscription");
        assertEquals(null, audioSink);
        assertEquals(null, pipelineSubscription);
    }

    private String conversationUuid(WebSocketSession session) {
        return session.getUri().getPath().substring(session.getUri().getPath().lastIndexOf('/') + 1);
    }
}
