<template>
  <section class="profile-list" data-test="profile-recents">
    <h3>最近会话</h3>
    <article v-for="item in items" :key="item.id" class="profile-list__item">
      <strong>{{ item.title }}</strong>
      <span>{{ item.subtitle }}</span>
    </article>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{ id: string; title: string; subtitle: string }>
}>()
</script>

<style scoped lang="scss">
.profile-list {
  display: grid;
  gap: 12px;
  padding: 24px;
  border-radius: var(--vt-radius-xl);
  background: var(--vt-surface);
  box-shadow: var(--vt-shadow);
}

.profile-list h3,
.profile-list__item strong,
.profile-list__item span {
  margin: 0;
}

.profile-list h3 {
  font-size: 20px;
  line-height: 1.2;
}

.profile-list__item {
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--vt-surface) 80%, var(--vt-brand) 8%);
}

.profile-list__item strong {
  font-size: 15px;
  line-height: 1.35;
}

.profile-list__item span {
  color: var(--vt-text-soft);
  font-size: 13px;
  line-height: 1.5;
}
</style>
