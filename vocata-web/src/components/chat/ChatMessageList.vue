<template>
  <section class="chat-message-list">
    <article
      v-for="(item, index) in messages"
      :key="item.messageUuid || index"
      class="chat-message-list__item"
      :class="item.type === 'send' ? 'is-user' : 'is-ai'"
    >
      <div class="chat-message-list__avatar">
        <img
          v-if="item.type === 'receive' && characterAvatar"
          :src="characterAvatar"
          :alt="characterName"
        />
        <img v-else-if="item.type === 'send' && userAvatar" :src="userAvatar" :alt="userName" />
        <span v-else>{{ item.type === 'send' ? userInitial : characterInitial }}</span>
      </div>
      <div class="chat-message-list__bubble">
        <p>{{ item.content }}</p>
        <small v-if="item.createDate">{{ formatTime(item.createDate) }}</small>
      </div>
    </article>

    <div v-if="thinking" class="chat-message-list__thinking">
      <div class="chat-message-list__avatar">
        <img v-if="characterAvatar" :src="characterAvatar" :alt="characterName" />
        <span v-else>{{ characterInitial }}</span>
      </div>
      <div class="chat-message-list__thinking-bubble">
        <span></span><span></span><span></span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ChatMessage } from '@/types/common'

defineProps<{
  messages: ChatMessage[]
  thinking: boolean
  characterAvatar: string
  characterName: string
  characterInitial: string
  userAvatar: string
  userName: string
  userInitial: string
  formatTime: (dateString: string) => string
}>()
</script>

<style scoped lang="scss">
.chat-message-list {
  display: grid;
  align-content: start;
  gap: 18px;
  min-height: 100%;
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
}

.chat-message-list__item,
.chat-message-list__thinking {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.chat-message-list__item.is-user {
  flex-direction: row-reverse;
}

.chat-message-list__avatar {
  display: grid;
  width: 36px;
  height: 36px;
  flex: none;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.92);
  color: var(--vt-text-soft);
  font-size: 13px;
  font-weight: 700;
}

.chat-message-list__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.chat-message-list__bubble,
.chat-message-list__thinking-bubble {
  display: grid;
  gap: 6px;
  max-width: min(680px, 84%);
  padding: 12px 14px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid color-mix(in srgb, var(--vt-line) 58%, white);
}

.chat-message-list__item.is-user .chat-message-list__bubble {
  background: color-mix(in srgb, var(--vt-brand) 12%, white);
}

.chat-message-list__bubble p,
.chat-message-list__bubble small {
  margin: 0;
}

.chat-message-list__bubble p {
  font-size: 15px;
  line-height: 1.72;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-message-list__bubble small {
  color: var(--vt-text-soft);
  font-size: 12px;
}

.chat-message-list__thinking-bubble {
  display: flex;
  align-items: center;
  width: fit-content;
}

.chat-message-list__thinking-bubble span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--vt-brand);
  animation: pulse 1s infinite ease-in-out;
}

.chat-message-list__thinking-bubble span:nth-child(2) {
  animation-delay: 0.12s;
}

.chat-message-list__thinking-bubble span:nth-child(3) {
  animation-delay: 0.24s;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  50% {
    opacity: 1;
    transform: translateY(-2px);
  }
}
</style>
