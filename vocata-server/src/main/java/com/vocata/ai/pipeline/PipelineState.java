package com.vocata.ai.pipeline;

/**
 * 流式对话管线状态
 */
public enum PipelineState {
    /** 空闲，等待用户输入 */
    IDLE,
    /** 正在接收用户音频 / STT 识别中 */
    LISTENING,
    /** STT 完成，LLM 生成中 */
    PROCESSING,
    /** LLM + TTS 输出中，音频正在播放给客户端 */
    SPEAKING
}
