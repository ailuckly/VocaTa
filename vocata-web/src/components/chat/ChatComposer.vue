<template>
  <section class="chat-composer" data-test="chat-composer">
    <div class="chat-composer__box">
      <button
        type="button"
        class="chat-composer__tool"
        data-test="composer-mic"
        :title="recording ? '挂断通话' : '录音输入'"
        @click="$emit('toggleCall')"
      >
        <span class="chat-composer__icon">🎙</span>
      </button>

      <div class="chat-composer__field">
        <textarea
          :value="modelValue"
          :placeholder="connected ? '输入消息或开始语音陪聊…' : '连接中，请稍等…'"
          :disabled="!connected"
          @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
          @keydown.enter.prevent="$emit('send')"
        />
      </div>
      <div class="chat-composer__actions">
        <button
          type="button"
          class="chat-composer__primary"
          data-test="composer-primary"
          :aria-label="hasText ? '发送消息' : '开始语音聊天'"
          :disabled="!connected"
          @click="handlePrimaryAction"
        >
          <span class="chat-composer__icon">{{ hasText ? '↑' : recording ? '■' : '◉' }}</span>
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
  display: block;
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
}

.chat-composer__box {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: end;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 32px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid color-mix(in srgb, var(--vt-line) 72%, white);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.chat-composer__tool,
.chat-composer__primary {
  width: 48px;
  height: 48px;
  border: 0;
  border-radius: 50%;
  display: inline-grid;
  place-items: center;
  cursor: pointer;
}

.chat-composer__tool {
  background: transparent;
  color: var(--vt-text-soft);
}

.chat-composer__field {
  min-width: 0;
}

.chat-composer__field textarea {
  width: 100%;
  min-height: 28px;
  max-height: 120px;
  border: 0;
  outline: 0;
  resize: none;
  background: transparent;
  color: var(--vt-text);
  font-size: 15px;
  line-height: 1.6;
}

.chat-composer__actions {
  display: flex;
  align-items: flex-end;
}

.chat-composer__primary {
  background: var(--vt-brand);
  color: white;
}

.chat-composer__primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.chat-composer__icon {
  display: inline-grid;
  place-items: center;
  width: 20px;
  height: 20px;
  font-size: 18px;
  line-height: 1;
}

@media (max-width: 768px) {
  .chat-composer__box {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }
}
</style>
