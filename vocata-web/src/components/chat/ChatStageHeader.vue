<template>
  <header class="chat-stage-header" data-test="chat-stage-header">
    <div class="chat-stage-header__identity">
      <div class="chat-stage-header__avatar">
        <img v-if="avatar" :src="avatar" :alt="characterName" />
        <span v-else>{{ initials }}</span>
      </div>
      <div class="chat-stage-header__copy">
        <p>{{ eyebrow }}</p>
        <h1>{{ characterName }}</h1>
      </div>
    </div>
    <div class="chat-stage-header__status" :class="{ 'is-connected': connected }">
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
  padding: 4px 0 8px;
  border-bottom: 1px solid color-mix(in srgb, var(--vt-line) 72%, white);
  max-width: 760px;
  margin: 0 auto;
}

.chat-stage-header__identity {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.chat-stage-header__avatar {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  background: color-mix(in srgb, var(--vt-brand) 18%, white);
  color: var(--vt-brand-strong);
  font-size: 13px;
  font-weight: 700;
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
  color: var(--vt-text-soft);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.chat-stage-header__copy h1 {
  font-size: 15px;
  line-height: 1.2;
  font-weight: 700;
}

.chat-stage-header__status {
  padding: 6px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--vt-surface) 88%, var(--vt-brand) 8%);
  color: var(--vt-text-soft);
  font-size: 11px;
  white-space: nowrap;
}

.chat-stage-header__status.is-connected {
  background: color-mix(in srgb, var(--vt-brand) 14%, white);
  color: var(--vt-brand-strong);
}
</style>
