<template>
  <Transition name="voice-panel">
    <section v-if="active" class="voice-call-panel" data-test="voice-panel">
      <div class="voice-call-panel__header">
        <div class="voice-call-panel__indicator" :class="{ 'is-speaking': speaking, 'is-muted': muted }">
          <span class="voice-call-panel__pulse"></span>
        </div>
        <span class="voice-call-panel__status">{{ status }}</span>
        <div class="voice-call-panel__actions">
          <button
            type="button"
            class="voice-call-panel__btn"
            :class="{ 'is-muted': muted }"
            :title="muted ? '取消静音' : '静音'"
            @click="$emit('mute')"
          >
            <el-icon v-if="muted"><MicrophoneOff /></el-icon>
            <el-icon v-else><Microphone /></el-icon>
          </button>
          <button
            type="button"
            class="voice-call-panel__btn is-hangup"
            title="结束通话"
            @click="$emit('hangup')"
          >
            <el-icon><PhoneFilled /></el-icon>
          </button>
        </div>
      </div>

      <div v-if="entries.length" class="voice-call-panel__transcripts">
        <div
          v-for="entry in entries.slice(-3)"
          :key="entry.timestamp"
          class="voice-call-panel__entry"
          :class="entry.speaker === 'user' ? 'is-user' : 'is-ai'"
        >
          <span class="voice-call-panel__speaker">{{ entry.speaker === 'user' ? '我' : characterName }}</span>
          <span class="voice-call-panel__text">{{ entry.text }}</span>
        </div>
      </div>
    </section>
  </Transition>
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
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  padding: 10px 14px;
  border-radius: var(--vt-radius-lg);
  background: var(--vt-surface);
  border: 1px solid var(--vt-line);
  box-shadow: var(--vt-shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* Transition */
.voice-panel-enter-active,
.voice-panel-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.voice-panel-enter-from,
.voice-panel-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

/* Header row */
.voice-call-panel__header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.voice-call-panel__indicator {
  position: relative;
  width: 10px;
  height: 10px;
  flex-shrink: 0;
}

.voice-call-panel__pulse {
  display: block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--vt-text-muted);

  .is-speaking & {
    background: var(--vt-brand);
    animation: pulse-ring 1.4s ease-out infinite;
  }

  .is-muted & {
    background: var(--vt-danger);
    animation: none;
  }
}

@keyframes pulse-ring {
  0% { box-shadow: 0 0 0 0 color-mix(in srgb, var(--vt-brand) 60%, transparent); }
  70% { box-shadow: 0 0 0 8px transparent; }
  100% { box-shadow: 0 0 0 0 transparent; }
}

.voice-call-panel__status {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: var(--vt-text-soft);
}

/* Action buttons */
.voice-call-panel__actions {
  display: flex;
  gap: 6px;
}

.voice-call-panel__btn {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  background: var(--vt-surface-overlay);
  color: var(--vt-text-soft);
  cursor: pointer;
  font-size: 15px;
  transition: background 0.15s, color 0.15s;

  &:hover {
    background: var(--vt-line);
    color: var(--vt-text);
  }

  &.is-muted {
    background: color-mix(in srgb, var(--vt-danger) 15%, transparent);
    color: var(--vt-danger);
  }

  &.is-hangup {
    background: var(--vt-danger);
    color: white;
    transform: rotate(135deg);

    &:hover {
      background: oklch(50% 0.22 25);
    }
  }
}

/* Transcripts */
.voice-call-panel__transcripts {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 6px;
  border-top: 1px solid var(--vt-line-subtle);
}

.voice-call-panel__entry {
  display: flex;
  gap: 6px;
  font-size: 13px;
  line-height: 1.5;
}

.voice-call-panel__speaker {
  font-weight: 600;
  flex-shrink: 0;
  color: var(--vt-text-soft);

  .is-ai & { color: var(--vt-brand); }
}

.voice-call-panel__text {
  color: var(--vt-text-soft);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
