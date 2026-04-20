<template>
  <section class="app-sidebar-profile">
    <RouterLink class="app-sidebar-profile__home" to="/profile">
      <div class="app-sidebar-profile__avatar">
        <img v-if="avatar" :src="avatar" :alt="displayName" />
        <span v-else>{{ initials }}</span>
      </div>
      <div class="app-sidebar-profile__meta">
        <strong>{{ displayName }}</strong>
        <span>我的陪伴空间</span>
      </div>
    </RouterLink>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  avatar?: string
  displayName: string
}>()

const initials = computed(() => (props.displayName || '语').slice(0, 1))
</script>

<style scoped lang="scss">
.app-sidebar-profile__home {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 18px;
  background: var(--vt-surface);
  box-shadow: var(--vt-shadow);
}

.app-sidebar-profile__avatar {
  display: grid;
  width: 40px;
  height: 40px;
  flex: none;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  background: color-mix(in srgb, var(--vt-brand) 20%, white);
  color: var(--vt-brand-strong);
  font-weight: 700;
}

.app-sidebar-profile__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-sidebar-profile__meta {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.app-sidebar-profile__meta strong,
.app-sidebar-profile__meta span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-sidebar-profile__meta strong {
  font-size: 14px;
  line-height: 1.3;
}

.app-sidebar-profile__meta span {
  color: var(--vt-text-soft);
  font-size: 11px;
}
</style>
