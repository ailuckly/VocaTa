<template>
  <section class="chat-composer" data-test="chat-composer">
    <div class="chat-composer__box">
      <button
        type="button"
        class="chat-composer__tool"
        data-test="composer-mic"
        :title="recording ? '挂断通话' : '开始语音对话'"
        @click="$emit('toggleCall')"
      >
        <el-icon><Microphone /></el-icon>
      </button>

      <div class="chat-composer__field">
        <textarea
          :value="modelValue"
          :placeholder="connected ? '输入消息或开始语音陪聊…' : '连接中，请稍等…'"
          :disabled="!connected"
          rows="1"
          @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
          @keydown.enter.prevent="$emit('send')"
        />
      </div>

      <div class="chat-composer__actions">
        <button
          type="button"
          class="chat-composer__primary"
          data-test="composer-primary"
          :class="{ 'is-active': hasText }"
          :aria-label="hasText ? '发送消息' : '开始语音聊天'"
          :disabled="!connected"
          @click="handlePrimaryAction"
        >
          <el-icon v-if="hasText"><Promotion /></el-icon>
          <el-icon v-else-if="recording"><VideoPause /></el-icon>
          <el-icon v-else><Microphone /></el-icon>
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  modelValue: string
  connected: boolean
  recording: boolean
}>()

const emit = defineEmits<{
  send: []
  toggleCall: []
  'update:modelValue': [value: string]
}>()

const hasText = computed(() => props.modelValue.trim().length > 0)

const handlePrimaryAction = () => {
  if (!props.connected) return
  if (hasText.value) {
    emit('send')
    return
  }
  emit('toggleCall')
}
</script>

<style scoped lang="scss">
.chat-composer {
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
}

.chat-composer__box {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: end;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 28px;
  background: var(--vt-surface);
  border: 1px solid var(--vt-line);
  box-shadow: var(--vt-shadow-sm);
  transition: border-color 0.15s, box-shadow 0.15s;

  &:focus-within {
    border-color: var(--vt-brand);
    box-shadow: 0 0 0 3px var(--vt-brand-soft);
  }
}

.chat-composer__tool {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--vt-text-soft);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  font-size: 18px;

  &:hover {
    background: var(--vt-surface-overlay);
    color: var(--vt-text);
  }
}

.chat-composer__field {
  min-width: 0;

  textarea {
    width: 100%;
    min-height: 24px;
    max-height: 120px;
    border: 0;
    outline: 0;
    resize: none;
    background: transparent;
    color: var(--vt-text);
    font-size: 15px;
    line-height: 1.6;
    padding: 8px 0;

    &::placeholder {
      color: var(--vt-text-muted);
    }
  }
}

.chat-composer__actions {
  display: flex;
  align-items: flex-end;
}

.chat-composer__primary {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 50%;
  background: var(--vt-surface-overlay);
  color: var(--vt-text-soft);
  cursor: pointer;
  font-size: 18px;
  transition: background 0.15s, color 0.15s;

  &.is-active {
    background: var(--vt-brand);
    color: white;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}
</style>
