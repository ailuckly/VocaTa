<template>
  <section class="role-shelf" data-test="role-shelf">
    <article v-for="role in roles" :key="role.id" class="role-shelf__card" data-test="role-card">
      <div class="role-shelf__media">
        <img v-if="role.avatarUrl" :src="role.avatarUrl" :alt="role.name || '角色头像'" />
      </div>
      <div class="role-shelf__body">
        <strong data-test="role-card-front">{{ role.name || '未命名角色' }}</strong>
        <p>{{ role.greeting || role.description || '开始一段轻松的陪伴聊天。' }}</p>
        <div class="role-shelf__meta">已聊天 {{ role.chatCount || 0 }} 次</div>
        <div class="role-shelf__actions">
          <button type="button" @click="$emit('detail', role)">详情</button>
          <button type="button" @click="$emit('start', role.id)">开始对话</button>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import type { roleInfo } from '@/types/common'

defineProps<{
  roles: roleInfo[]
}>()

defineEmits<{
  detail: [role: roleInfo]
  start: [characterId: string | number]
}>()
</script>

<style scoped lang="scss">
.role-shelf {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.role-shelf__card {
  display: grid;
  grid-template-rows: 220px auto;
  overflow: hidden;
  border-radius: 24px;
  background: var(--vt-surface);
  box-shadow: var(--vt-shadow);
  min-width: 0;
}

.role-shelf__media {
  background: color-mix(in srgb, var(--vt-brand) 14%, white);
}

.role-shelf__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.role-shelf__body {
  display: grid;
  align-content: start;
  gap: 10px;
  padding: 18px;
  min-width: 0;
}

.role-shelf__body strong {
  font-size: 18px;
  line-height: 1.25;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-shelf__body p,
.role-shelf__meta {
  margin: 0;
  color: var(--vt-text-soft);
}

.role-shelf__body p {
  font-size: 14px;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.role-shelf__meta {
  font-size: 12px;
}

.role-shelf__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.role-shelf__actions button {
  border: 0;
  border-radius: 999px;
  padding: 9px 14px;
  cursor: pointer;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
}

.role-shelf__actions button:last-child {
  background: var(--vt-brand);
  color: white;
}

@media (max-width: 1200px) {
  .role-shelf {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .role-shelf {
    grid-template-columns: 1fr;
  }
}
</style>
