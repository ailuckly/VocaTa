<template>
  <header class="discovery-hero" data-test="discovery-hero">
    <div class="discovery-hero__copy">
      <p>Companion foyer</p>
      <h1>找到今天适合开口的陪伴对象</h1>
      <span>从一个愿意接住你的角色开始，立刻进入对话。</span>
    </div>
    <div class="discovery-hero__featured">
      <article
        v-for="role in selectedRoles.slice(0, 3)"
        :key="role.id"
        class="discovery-hero__card"
      >
        <strong>{{ role.name || '精选角色' }}</strong>
        <p>{{ role.greeting || role.description || '开始一段轻松的陪伴聊天。' }}</p>
        <button type="button" @click="$emit('start', role.id)">开始对话</button>
      </article>
    </div>
  </header>
</template>

<script setup lang="ts">
import type { roleInfo } from '@/types/common'

defineProps<{
  selectedRoles: roleInfo[]
}>()

defineEmits<{
  start: [characterId: string | number]
}>()
</script>

<style scoped lang="scss">
.discovery-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(380px, 0.85fr);
  gap: 24px;
  padding: 32px;
  border-radius: var(--vt-radius-xl);
  background: linear-gradient(135deg, color-mix(in srgb, var(--vt-brand) 16%, white), var(--vt-surface));
  box-shadow: var(--vt-shadow);
  align-items: start;
}

.discovery-hero__copy p,
.discovery-hero__copy span {
  margin: 0;
  color: var(--vt-text-soft);
}

.discovery-hero__copy p {
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 12px;
}

.discovery-hero__copy h1 {
  margin: 0 0 12px;
  max-width: 8ch;
  font-size: clamp(24px, 2.4vw, 36px);
  line-height: 1.08;
  font-weight: 700;
}

.discovery-hero__copy span {
  display: block;
  max-width: 28ch;
  font-size: 15px;
  line-height: 1.6;
}

.discovery-hero__featured {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.discovery-hero__card {
  display: grid;
  gap: 8px;
  padding: 18px;
  border-radius: 20px;
  background: var(--vt-surface);
  min-width: 0;
}

.discovery-hero__card strong {
  font-size: 16px;
  line-height: 1.15;
  font-weight: 700;
}

.discovery-hero__card p {
  margin: 0;
  color: var(--vt-text-soft);
  font-size: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.discovery-hero__card button {
  justify-self: start;
  border: 0;
  border-radius: 999px;
  padding: 9px 14px;
  background: var(--vt-brand);
  color: white;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 960px) {
  .discovery-hero {
    grid-template-columns: 1fr;
    padding: 24px;
  }
}
</style>
