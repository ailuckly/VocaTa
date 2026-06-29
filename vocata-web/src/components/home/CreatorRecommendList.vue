<template>
  <div class="creator-list">
    <article v-for="creator in creators" :key="creator.name">
      <span data-test="creator-avatar">
        <img :src="creator.avatar" :alt="creator.name" @error="onAvatarError($event, creator.name)" />
      </span>
      <div>
        <strong>{{ creator.name }}</strong>
        <small>{{ creator.skill }} · 粉丝 {{ creator.fans }}</small>
      </div>
      <button type="button" :class="{ 'is-followed': followedNames.includes(creator.name) }" @click="$emit('follow', creator.name)">
        {{ followedNames.includes(creator.name) ? '已关注' : '关注' }}
      </button>
    </article>
  </div>
</template>

<script setup lang="ts">
import type { CreatorData } from './homeData'
import { onAvatarError } from '@/utils/avatar'

defineProps<{
  creators: CreatorData[]
  followedNames: string[]
}>()

defineEmits<{
  follow: [name: string]
}>()
</script>

<style scoped lang="scss">
.creator-list {
  display: grid;
  gap: 13px;
}

.creator-list article {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 48px;
}

.creator-list span {
  display: block;
  width: 40px;
  height: 40px;
  overflow: hidden;
  border-radius: 50%;
  background: #f7f8fa;
}

.creator-list img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.creator-list div {
  min-width: 0;
}

.creator-list strong,
.creator-list small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.creator-list strong {
  color: #171b33;
  font-size: 13px;
  font-weight: 600;
}

.creator-list small {
  margin-top: 4px;
  color: #a0a5b5;
  font-size: 12px;
}

.creator-list button {
  border: 1px solid rgba(107, 71, 245, 0.4);
  border-radius: 999px;
  padding: 4px 14px;
  color: #6b47f5;
  background: white;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s;
  
  &:hover {
    background: #f0ecff;
  }

  &.is-followed {
    border-color: #e2e6f5;
    color: #a0a5b5;
    background: #f7f8fa;
  }
}
</style>
