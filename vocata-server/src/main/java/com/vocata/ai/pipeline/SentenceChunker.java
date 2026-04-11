package com.vocata.ai.pipeline;

import com.vocata.ai.dto.UnifiedAiStreamChunk;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 将 LLM token 流切分为句子级文本块，供 TTS 合成使用。
 *
 * 切句规则:
 * 1. 遇到句末标点（。！？.!?；;）立即切
 * 2. 遇到逗号/顿号且已累积超过 MIN_CHARS_ON_COMMA 个字符时切
 * 3. 累积超过 MAX_CHARS_FORCE 个字符时强制切（避免超长句）
 * 4. 流结束时把剩余内容作为最后一个句子
 */
public class SentenceChunker {

    /** 遇到这些标点立即切句 */
    private static final Set<Character> SENTENCE_ENDERS = Set.of(
            '。', '！', '？', '.', '!', '?', '；', ';'
    );

    /** 遇到逗号类标点且累积超过此长度时切句 */
    private static final Set<Character> SOFT_BREAKERS = Set.of(
            '，', ',', '、', '：', ':'
    );

    private static final int MIN_CHARS_ON_COMMA = 30;
    private static final int MAX_CHARS_FORCE = 80;

    /**
     * 将 LLM token 流转换为句子流
     *
     * @param tokenStream LLM 输出的 UnifiedAiStreamChunk 流
     * @return 每个元素是一个可独立送 TTS 合成的句子文本
     */
    public static Flux<String> chunkToSentences(Flux<UnifiedAiStreamChunk> tokenStream) {
        return Flux.create(sink -> {
            StringBuilder buffer = new StringBuilder();

            tokenStream.subscribe(
                    chunk -> {
                        String content = chunk.getContent();
                        if (content == null || content.isEmpty()) {
                            return;
                        }

                        for (int i = 0; i < content.length(); i++) {
                            char c = content.charAt(i);
                            buffer.append(c);

                            boolean shouldCut = false;

                            if (SENTENCE_ENDERS.contains(c)) {
                                // 句末标点 → 立即切
                                shouldCut = true;
                            } else if (SOFT_BREAKERS.contains(c) && buffer.length() >= MIN_CHARS_ON_COMMA) {
                                // 逗号类标点 + 已累积够长 → 切
                                shouldCut = true;
                            } else if (buffer.length() >= MAX_CHARS_FORCE) {
                                // 强制切（防止超长无标点文本）
                                shouldCut = true;
                            }

                            if (shouldCut) {
                                String sentence = buffer.toString().trim();
                                if (!sentence.isEmpty()) {
                                    sink.next(sentence);
                                }
                                buffer.setLength(0);
                            }
                        }
                    },
                    error -> sink.error(error),
                    () -> {
                        // 流结束时输出剩余内容
                        String remaining = buffer.toString().trim();
                        if (!remaining.isEmpty()) {
                            sink.next(remaining);
                        }
                        sink.complete();
                    }
            );
        });
    }

    private SentenceChunker() {} // 工具类，不实例化
}
