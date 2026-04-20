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
  padding: 24px;
  border-radius: var(--vt-radius-xl);
  background: var(--vt-surface);
  box-shadow: var(--vt-shadow);
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
}

.profile-card h2 {
  margin: 0;
  font-size: 32px;
  line-height: 1.1;
  font-weight: 700;
}

.profile-card span {
  max-width: 30ch;
  font-size: 15px;
  line-height: 1.6;
}
</style>
