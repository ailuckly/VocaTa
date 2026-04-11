package com.vocata.ai.pipeline;

import com.vocata.ai.dto.UnifiedAiStreamChunk;

import java.util.Map;

/**
 * 流式管线事件 —— 类型安全地替代原来的 Map&lt;String, Object&gt;
 * 从 WebSocket handler → 客户端的所有消息类型都在这里定义。
 */
public abstract class PipelineEvent {

    private final String type;
    private final long timestamp;

    protected PipelineEvent(String type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }

    // ───────── STT 识别结果 ─────────

    public static class SttResult extends PipelineEvent {
        private final String text;
        private final boolean isFinal;
        private final double confidence;

        public SttResult(String text, boolean isFinal, double confidence) {
            super("stt_result");
            this.text = text;
            this.isFinal = isFinal;
            this.confidence = confidence;
        }

        public String getText() { return text; }
        public boolean isFinal() { return isFinal; }
        public double getConfidence() { return confidence; }
    }

    // ───────── LLM 文本流 chunk ─────────

    public static class LlmTextChunk extends PipelineEvent {
        private final String text;
        private final String accumulatedText;
        private final boolean isComplete;
        private final String characterName;

        public LlmTextChunk(String text, String accumulatedText, boolean isComplete, String characterName) {
            super("llm_text_stream");
            this.text = text;
            this.accumulatedText = accumulatedText;
            this.isComplete = isComplete;
            this.characterName = characterName;
        }

        public String getText() { return text; }
        public String getAccumulatedText() { return accumulatedText; }
        public boolean isComplete() { return isComplete; }
        public String getCharacterName() { return characterName; }
    }

    // ───────── TTS 句子级音频 ─────────

    public static class SentenceAudio extends PipelineEvent {
        private final byte[] audioData;
        private final String correspondingText;
        private final int sentenceIndex;
        private final String audioFormat;
        private final int sampleRate;

        public SentenceAudio(byte[] audioData, String correspondingText, int sentenceIndex,
                             String audioFormat, int sampleRate) {
            super("sentence_audio");
            this.audioData = audioData;
            this.correspondingText = correspondingText;
            this.sentenceIndex = sentenceIndex;
            this.audioFormat = audioFormat;
            this.sampleRate = sampleRate;
        }

        public byte[] getAudioData() { return audioData; }
        public String getCorrespondingText() { return correspondingText; }
        public int getSentenceIndex() { return sentenceIndex; }
        public String getAudioFormat() { return audioFormat; }
        public int getSampleRate() { return sampleRate; }
    }

    // ───────── 音频元数据（在二进制帧之前发送） ─────────

    public static class AudioMeta extends PipelineEvent {
        private final int audioSize;
        private final String format;
        private final int sampleRate;

        public AudioMeta(int audioSize, String format, int sampleRate) {
            super("tts_audio_meta");
            this.audioSize = audioSize;
            this.format = format;
            this.sampleRate = sampleRate;
        }

        public int getAudioSize() { return audioSize; }
        public String getFormat() { return format; }
        public int getSampleRate() { return sampleRate; }
    }

    // ───────── Barge-in 确认 ─────────

    public static class BargeInAck extends PipelineEvent {
        private final String truncatedText;

        public BargeInAck(String truncatedText) {
            super("barge_in_ack");
            this.truncatedText = truncatedText;
        }

        public String getTruncatedText() { return truncatedText; }
    }

    // ───────── 管线状态变更通知 ─────────

    public static class StateChange extends PipelineEvent {
        private final PipelineState state;

        public StateChange(PipelineState state) {
            super("pipeline_state");
            this.state = state;
        }

        public PipelineState getState() { return state; }
    }

    // ───────── 状态 / 完成 / 错误 ─────────

    public static class Status extends PipelineEvent {
        private final String message;

        public Status(String message) {
            super("status");
            this.message = message;
        }

        public String getMessage() { return message; }
    }

    public static class Complete extends PipelineEvent {
        public Complete() {
            super("complete");
        }
    }

    public static class Error extends PipelineEvent {
        private final String error;

        public Error(String error) {
            super("error");
            this.error = error;
        }

        public String getError() { return error; }
    }
}
