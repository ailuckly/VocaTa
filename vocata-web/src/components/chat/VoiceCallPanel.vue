<template>
  <section class="voice-call-panel" data-test="voice-panel" v-show="active">
    <div class="voice-call-panel__summary">
      <strong>{{ status }}</strong>
      <span>{{ speaking ? 'AI 正在回答' : '语音通道已就绪' }}</span>
    </div>
    <div v-if="entries.length" class="voice-call-panel__chips">
      <article v-for="entry in entries.slice(-3)" :key="entry.timestamp" class="voice-call-panel__chip">
        <strong>{{ entry.speaker === 'user' ? '我' : characterName }}</strong>
        <span>{{ entry.text }}</span>
      </article>
    </div>
    <div class="voice-call-panel__actions">
      <button type="button" @click="$emit('mute')">{{ muted ? '取消静音' : '静音' }}</button>
      <button type="button" @click="$emit('hangup')">结束通话</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { VoiceTranscriptItem } from '@/types/ui'

defineProps<{
  active: boolean
  entries: VoiceTranscriptItem[]
  speaking: boolean
  muted: boolean
  status: string
  characterName: string
}>()

defineEmits<{
  mute: []
  hangup: []
}>()
</script>

<style scoped lang="scss">
.voice-call-panel {
  display: grid;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 18px;
  background: color-mix(in srgb, var(--vt-surface) 92%, var(--vt-brand) 6%);
  border: 1px solid color-mix(in srgb, var(--vt-line) 64%, white);
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
}

.voice-call-panel__summary {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.voice-call-panel__summary strong {
  font-size: 13px;
}

.voice-call-panel__summary span {
  color: var(--vt-text-soft);
  font-size: 12px;
}

.voice-call-panel__chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.voice-call-panel__chip {
  display: grid;
  gap: 2px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.84);
}

.voice-call-panel__chip strong {
  font-size: 12px;
}

.voice-call-panel__chip span {
  max-width: 28ch;
  color: var(--vt-text-soft);
  font-size: 12px;
  line-height: 1.45;
}

.voice-call-panel__actions {
  display: flex;
  gap: 10px;
}

.voice-call-panel__actions button {
  border: 0;
  border-radius: 999px;
  padding: 9px 14px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--vt-text);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}
</style>
