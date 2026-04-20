<template>
  <section class="app-sidebar-history">
    <div class="app-sidebar-history__header">
      <h2>最近对话</h2>
    </div>

    <div v-if="items.length" class="app-sidebar-history__list">
      <article
        v-for="item in items"
        :key="item.conversationUuid"
        class="app-sidebar-history__item"
        :class="{ 'is-active': item.conversationUuid === activeConversationUuid }"
        @click="$emit('open', item.conversationUuid)"
      >
        <div class="app-sidebar-history__avatar">
          <img v-if="item.characterAvatarUrl" :src="item.characterAvatarUrl" :alt="item.characterName || item.title || '角色头像'" />
          <span v-else>{{ (item.characterName || item.title || '语').slice(0, 1) }}</span>
        </div>
        <div class="app-sidebar-history__body">
          <strong>{{ item.title || item.characterName || '未命名对话' }}</strong>
          <span>{{ item.lastMessageSummary || item.greeting || '继续上一次对话。' }}</span>
        </div>
      </article>
    </div>

    <div v-else class="app-sidebar-history__empty">
      还没有历史对话，从探索页开始第一段陪伴聊天。
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ConversationResponse } from '@/types/api'

defineProps<{
  items: ConversationResponse[]
  activeConversationUuid: string
}>()

defineEmits<{
  open: [conversationUuid: string]
}>()
</script>

<style scoped lang="scss">
.app-sidebar-history {
  display: grid;
  gap: 10px;
  min-height: 0;
}

.app-sidebar-history__header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-sidebar-history__header h2 {
  margin: 0;
  font-size: 13px;
  line-height: 1.3;
  color: var(--vt-text-soft);
}

.app-sidebar-history__body span,
.app-sidebar-history__empty {
  color: var(--vt-text-soft);
}

.app-sidebar-history__list {
  display: grid;
  gap: 8px;
}

.app-sidebar-history__item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--vt-surface) 72%, var(--vt-brand) 8%);
  border: 1px solid var(--vt-line);
  cursor: pointer;
  min-width: 0;
}

.app-sidebar-history__item.is-active {
  background: var(--vt-surface);
  box-shadow: var(--vt-shadow);
}

.app-sidebar-history__avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex: none;
  place-items: center;
  border-radius: 12px;
  overflow: hidden;
  background: var(--vt-surface);
  font-size: 12px;
  font-weight: 700;
}

.app-sidebar-history__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-sidebar-history__body {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.app-sidebar-history__body strong,
.app-sidebar-history__body span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-sidebar-history__body strong {
  font-size: 13px;
  line-height: 1.3;
}

.app-sidebar-history__body span {
  font-size: 11px;
}

.app-sidebar-history__empty {
  padding: 12px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--vt-surface) 72%, var(--vt-brand) 8%);
  font-size: 12px;
  line-height: 1.5;
}
</style>
