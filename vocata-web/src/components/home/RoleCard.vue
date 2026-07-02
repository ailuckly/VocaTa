<template>
  <article class="role-card" :class="{ 'is-soft': variant === 'soft' }" @click="$emit('detail', role.id)">
    <div class="role-card__avatar">
      <img :src="role.avatar" :alt="role.name" @error="onAvatarError($event, role.name)" />
    </div>
    <div class="role-card__body">
      <strong>{{ role.name }}</strong>
      <span>{{ role.desc }}</span>
      <small>👤 {{ role.heat }}</small>
    </div>
  </article>
</template>

<script setup lang="ts">
import type { HomeRoleCardData } from './homeData'
import { onAvatarError } from '@/utils/avatar'

withDefaults(defineProps<{
  role: HomeRoleCardData
  variant?: 'default' | 'soft'
}>(), {
  variant: 'default',
})

defineEmits<{
  chat: [id: number]
  detail: [id: number]
}>()
</script>

<style scoped lang="scss">
.role-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  border: 1px solid rgba(129, 140, 248, 0.14);
  border-radius: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 10px 24px rgba(75, 83, 150, 0.04);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  cursor: pointer;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 32px rgba(75, 83, 150, 0.08);
  }

  &.is-soft {
    background: rgba(255, 255, 255, 0.6);
  }
}

.role-card__avatar {
  align-self: start;
  width: 44px;
  height: 44px;
  overflow: hidden;
  border-radius: 50%;
  background: #f0ecff;
  border: 1px solid rgba(129, 140, 248, 0.14);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.role-card__body {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.role-card__body strong,
.role-card__body span,
.role-card__body small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-card__body strong {
  color: #18213d;
  font-size: 14px;
  font-weight: 600;
}

.role-card__body span {
  margin-top: 4px;
  color: #7d849d;
  font-size: 13px;
}

.role-card__body small {
  margin-top: 6px;
  color: #a0a5b5;
  font-size: 12px;
}
</style>
