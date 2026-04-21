<template>
  <header class="chat-stage-header" data-test="chat-stage-header">
    <div class="chat-stage-header__identity">
      <div class="chat-stage-header__avatar">
        <img v-if="avatar" :src="avatar" :alt="characterName" />
        <span v-else>{{ initials }}</span>
      </div>
      <div class="chat-stage-header__copy">
        <p v-if="eyebrow">{{ eyebrow }}</p>
        <h1>{{ characterName }}</h1>
      </div>
    </div>
    <div class="chat-stage-header__status" :class="{ 'is-connected': connected }">
      <span class="chat-stage-header__dot"></span>
      {{ status }}
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  avatar: string
  characterName: string
  connected: boolean
  status: string
  eyebrow?: string
}>()

const initials = computed(() => props.characterName.slice(0, 2).toUpperCase())
</script>

<style scoped lang="scss">
.chat-stage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--vt-line);
  max-width: 760px;
  margin: 0 auto;
  width: 100%;
}

.chat-stage-header__identity {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.chat-stage-header__avatar {
  display: grid;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  background: var(--vt-brand-soft);
  color: var(--vt-brand-strong);
  font-size: 13px;
  font-weight: 700;
  border: 1.5px solid var(--vt-line);
}

.chat-stage-header__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.chat-stage-header__copy {
  min-width: 0;
}

.chat-stage-header__copy p,
.chat-stage-header__copy h1 {
  margin: 0;
}

.chat-stage-header__copy p {
  color: var(--vt-text-muted);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.chat-stage-header__copy h1 {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--vt-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-stage-header__status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--vt-surface-overlay);
  color: var(--vt-text-muted);
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.chat-stage-header__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--vt-text-muted);
  flex-shrink: 0;
}

.chat-stage-header__status.is-connected {
  background: var(--vt-brand-soft);
  color: var(--vt-brand-strong);

  .chat-stage-header__dot {
    background: var(--vt-brand);
  }
}
</style>
