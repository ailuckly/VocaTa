<template>
  <section class="continue-chat">
    <div class="home-section-heading">
      <h2>继续聊天</h2>
      <button type="button" @click="scrollNext">更多 &gt;</button>
    </div>
    <div class="continue-chat__grid" ref="scrollContainer">
      <article v-for="item in items" :key="item.id" class="continue-chat__card">
        <img :src="item.avatar" :alt="item.name" @error="onAvatarError($event, item.name)" />
        <div class="continue-chat__info">
          <strong>{{ item.name }}</strong>
          <span>{{ item.lastTopic }}</span>
        </div>
        <button type="button" @click="$emit('continue', item.id)">继续</button>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ContinueChatData } from './homeData'
import { onAvatarError } from '@/utils/avatar'
import { ref } from 'vue'

const scrollContainer = ref<HTMLElement | null>(null)

const scrollNext = () => {
  if (scrollContainer.value) {
    const el = scrollContainer.value
    const maxScroll = el.scrollWidth - el.clientWidth
    if (el.scrollLeft >= maxScroll - 10) {
      el.scrollTo({ left: 0, behavior: 'smooth' })
    } else {
      el.scrollBy({ left: 260, behavior: 'smooth' })
    }
  }
}

defineProps<{
  items: ContinueChatData[]
}>()

defineEmits<{
  continue: [id: number | string]
}>()
</script>

<style scoped lang="scss">
.continue-chat {
  display: grid;
  gap: 14px;
}

.continue-chat__grid {
  display: flex;
  overflow-x: auto;
  gap: 16px;
  padding-bottom: 8px;
  scroll-behavior: smooth;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}

.continue-chat__card {
  flex: 0 0 240px;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 72px;
  border: 1px solid rgba(129, 140, 248, 0.14);
  border-radius: 16px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 10px 24px rgba(75, 83, 150, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 32px rgba(75, 83, 150, 0.08);
  }
}

.continue-chat__card img {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}

.continue-chat__info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.continue-chat__info strong,
.continue-chat__info span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.continue-chat__info strong {
  color: #18213d;
  font-size: 14px;
  font-weight: 600;
}

.continue-chat__info span {
  margin-top: 4px;
  color: #7d849d;
  font-size: 12px;
}

.continue-chat__card button {
  flex-shrink: 0;
  border: 0;
  border-radius: 999px;
  padding: 5px 16px;
  color: #6b47f5;
  background: #f0ecff;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s;
  
  &:hover {
    background: #6b47f5;
    color: white;
  }
}

@media (max-width: 640px) {
  .continue-chat__card {
    flex: 0 0 200px;
  }
}
</style>
