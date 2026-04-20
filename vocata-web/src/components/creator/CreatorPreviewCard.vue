<template>
  <aside class="creator-preview" data-test="creator-preview">
    <p>创建预览</p>
    <div class="creator-preview__avatar">
      <img v-if="avatarUrl" :src="avatarUrl" :alt="name || '角色头像'" />
      <span v-else>{{ (name || '语').slice(0, 1) }}</span>
    </div>
    <strong>{{ name || '未命名角色' }}</strong>
    <span>{{ greeting || '这里会展示角色的第一句开场白。' }}</span>
    <div class="creator-preview__panel">
      {{ description || '这里会展示角色的人设和陪伴气质简介。' }}
    </div>
  </aside>
</template>

<script setup lang="ts">
defineProps<{
  name: string
  greeting: string
  description: string
  avatarUrl: string
}>()
</script>

<style scoped lang="scss">
.creator-preview {
  display: grid;
  gap: 14px;
  padding: 24px;
  border-radius: var(--vt-radius-xl);
  background: linear-gradient(180deg, color-mix(in srgb, var(--vt-brand) 16%, white), var(--vt-surface));
  box-shadow: var(--vt-shadow);
}

.creator-preview > p,
.creator-preview > span {
  margin: 0;
  color: var(--vt-text-soft);
}

.creator-preview__avatar {
  display: grid;
  width: 96px;
  height: 96px;
  place-items: center;
  border-radius: 28px;
  overflow: hidden;
  background: var(--vt-surface);
  font-size: 28px;
  font-weight: 700;
}

.creator-preview__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.creator-preview__panel {
  padding: 16px;
  border-radius: 18px;
  background: color-mix(in srgb, var(--vt-surface) 80%, var(--vt-brand) 8%);
}
</style>
