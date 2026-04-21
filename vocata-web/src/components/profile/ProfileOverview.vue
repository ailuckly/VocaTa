<template>
  <section class="profile-card" data-test="profile-overview">
    <p>我的空间</p>
    <h2>{{ name }}</h2>
    <span>{{ subtitle }}</span>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  name: string
  subtitle: string
}>()
</script>

<style scoped lang="scss">
.profile-card {
  display: grid;
  gap: 10px;
  padding: 28px;
  border-radius: var(--vt-radius-lg);
  background: var(--vt-surface);
  border: 1px solid var(--vt-line);
  box-shadow: var(--vt-shadow-sm);
}

.profile-card p,
.profile-card span {
  margin: 0;
  color: var(--vt-text-soft);
}

.profile-card p {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--vt-text-muted);
}

.profile-card h2 {
  margin: 0;
  font-size: 28px;
  line-height: 1.15;
  font-weight: 700;
  color: var(--vt-text);
}

.profile-card span {
  max-width: 36ch;
  font-size: 14px;
  line-height: 1.6;
}
</style>
