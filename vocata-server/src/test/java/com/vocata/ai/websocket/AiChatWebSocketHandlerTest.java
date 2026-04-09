package com.vocata.ai.websocket;

import com.vocata.ai.service.AiStreamingService;
import com.vocata.conversation.service.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatWebSocketHandlerTest {

    private final AiStreamingService aiStreamingService = mock(AiStreamingService.class);
    private final ConversationService conversationService = mock(ConversationService.class);

    private AiChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AiChatWebSocketHandler();
        ReflectionTestUtils.setField(handler, "aiStreamingService", aiStreamingService);
        ReflectionTestUtils.setField(handler, "conversationService", conversationService);
    }

    @Test
    void audioStartBeginsVoiceProcessingImmediatelyAndAudioEndOnlyCompletesStream() throws Exception {
        WebSocketSession session = mockVoiceSession();
        AtomicInteger subscriptions = new AtomicInteger();
        AtomicBoolean completed = new AtomicBoolean();

        when(aiStreamingService.processVoiceMessage(eq(conversationUuid(session)), eq("42"), any()))
                .thenAnswer(invocation -> {
                    Flux<byte[]> audioStream = invocation.getArgument(2);
                    return audioStream
                            .doOnSubscribe(ignored -> subscriptions.incrementAndGet())
                            .doOnComplete(() -> completed.set(true))
                            .thenMany(Flux.empty());
                });

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_start\"}"));

        assertEquals(1, subscriptions.get());
        verify(aiStreamingService, times(1))
                .processVoiceMessage(eq(conversationUuid(session)), eq("42"), any());

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_end\"}"));

        assertTrue(completed.get());
        verify(aiStreamingService, times(1))
                .processVoiceMessage(eq(conversationUuid(session)), eq("42"), any());
        assertTrue(voiceSessions().isEmpty());
    }

    @Test
    void audioCancelDisposesActiveVoiceSession() throws Exception {
        WebSocketSession session = mockVoiceSession();
        AtomicBoolean cancelled = new AtomicBoolean();

        when(aiStreamingService.processVoiceMessage(eq(conversationUuid(session)), eq("42"), any()))
                .thenAnswer(invocation -> {
                    Flux<byte[]> audioStream = invocation.getArgument(2);
                    return audioStream.thenMany(Flux.<Map<String, Object>>never()
                            .doOnCancel(() -> cancelled.set(true)));
                });

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_start\"}"));
        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_cancel\"}"));

        assertTrue(cancelled.get());
        assertTrue(voiceSessions().isEmpty());
    }

    @Test
    void afterConnectionClosedDisposesActiveVoiceSession() throws Exception {
        WebSocketSession session = mockVoiceSession();
        AtomicBoolean cancelled = new AtomicBoolean();

        when(aiStreamingService.processVoiceMessage(eq(conversationUuid(session)), eq("42"), any()))
                .thenAnswer(invocation -> {
                    Flux<byte[]> audioStream = invocation.getArgument(2);
                    return audioStream.thenMany(Flux.<Map<String, Object>>never()
                            .doOnCancel(() -> cancelled.set(true)));
                });

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_start\"}"));
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertTrue(cancelled.get());
        assertTrue(voiceSessions().isEmpty());
    }

    @Test
    void transportErrorDisposesActiveVoiceSession() throws Exception {
        WebSocketSession session = mockVoiceSession();
        AtomicBoolean cancelled = new AtomicBoolean();

        when(aiStreamingService.processVoiceMessage(eq(conversationUuid(session)), eq("42"), any()))
                .thenAnswer(invocation -> {
                    Flux<byte[]> audioStream = invocation.getArgument(2);
                    return audioStream.thenMany(Flux.<Map<String, Object>>never()
                            .doOnCancel(() -> cancelled.set(true)));
                });

        handler.handleMessage(session, new TextMessage("{\"type\":\"audio_start\"}"));
        handler.handleTransportError(session, new IOException("boom"));

        assertTrue(cancelled.get());
        assertTrue(voiceSessions().isEmpty());
    }

    private WebSocketSession mockVoiceSession() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("authenticatedUserId", "42");

        UUID conversationUuid = UUID.randomUUID();
        when(session.getId()).thenReturn("session-1");
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/chat/" + conversationUuid));
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        doNothing().when(session).sendMessage(any(WebSocketMessage.class));

        return session;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> voiceSessions() {
        Object voiceSessions = ReflectionTestUtils.getField(handler, "voiceSessions");
        return voiceSessions == null ? Map.of() : (Map<String, Object>) voiceSessions;
    }

    private String conversationUuid(WebSocketSession session) {
        return session.getUri().getPath().substring(session.getUri().getPath().lastIndexOf('/') + 1);
    }
}
