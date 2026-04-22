<template>
  <section class="chat-message-list">
    <article
      v-for="(item, index) in messages"
      :key="item.messageUuid || index"
      class="chat-message-list__item"
      :class="item.type === 'send' ? 'is-user' : 'is-ai'"
    >
      <div v-if="item.type === 'receive'" class="chat-message-list__avatar">
        <img v-if="characterAvatar" :src="characterAvatar" :alt="characterName"
        @error="onAvatarError($event, characterName)" />
        <span v-else>{{ characterInitial }}</span>
      </div>
      <div class="chat-message-list__bubble" :class="{ 'is-streaming': (item as any).isStreaming }">
        <p>{{ item.content }}</p>
        <span v-if="(item as any).isStreaming" class="chat-message-list__cursor" aria-hidden="true">|</span>
        <small v-if="item.createDate">{{ formatTime(item.createDate) }}</small>
      </div>
    </article>

    <div v-if="thinking" class="chat-message-list__item is-ai">
      <div class="chat-message-list__avatar">
        <img v-if="characterAvatar" :src="characterAvatar" :alt="characterName"
        @error="onAvatarError($event, characterName)" />
        <span v-else>{{ characterInitial }}</span>
      </div>
      <div class="chat-message-list__thinking">
        <span></span><span></span><span></span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ChatMessage } from '@/types/common'
import { onAvatarError } from '@/utils/avatar'

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
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  padding: 8px 0;
}

.chat-message-list__item {
  display: flex;
  gap: 10px;
  align-items: flex-end;

  &.is-user {
    flex-direction: row-reverse;
  }
}

.chat-message-list__avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  background: var(--vt-brand-soft);
  color: var(--vt-brand-strong);
  font-size: 12px;
  font-weight: 700;
  border: 1px solid var(--vt-line);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.chat-message-list__bubble {
  position: relative;
  max-width: min(640px, 82%);
  padding: 10px 14px;
  border-radius: 18px;
  background: var(--vt-surface-raised);
  border: 1px solid var(--vt-line);

  p {
    margin: 0;
    font-size: 15px;
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-word;
    color: var(--vt-text);
  }

  small {
    display: block;
    margin-top: 4px;
    color: var(--vt-text-muted);
    font-size: 11px;
  }

  .is-user & {
    background: var(--vt-brand-soft);
    border-color: transparent;

    p { color: var(--vt-brand-strong); }
  }
}

.chat-message-list__cursor {
  display: inline-block;
  margin-left: 1px;
  color: var(--vt-brand);
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.chat-message-list__thinking {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 12px 16px;
  border-radius: 18px;
  background: var(--vt-surface-raised);
  border: 1px solid var(--vt-line);

  span {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--vt-brand);
    animation: bounce 1.1s infinite ease-in-out;

    &:nth-child(2) { animation-delay: 0.14s; }
    &:nth-child(3) { animation-delay: 0.28s; }
  }
}

@keyframes bounce {
  0%, 100% { opacity: 0.3; transform: translateY(0); }
  50% { opacity: 1; transform: translateY(-3px); }
}
</style>
