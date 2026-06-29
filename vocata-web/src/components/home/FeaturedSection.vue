<template>
  <section class="home-section">
    <div class="home-section-heading">
      <h2>{{ title }}</h2>
      <button type="button" @click="scrollNext">更多 &gt;</button>
    </div>

    <div v-if="variant === 'visual'" class="featured-grid" ref="scrollContainer">
      <article v-for="item in featuredItems" :key="item.id" class="featured-card" @click="$emit('detail', item.id)">
        <img :src="item.cover" :alt="item.name" class="featured-card__bg" @error="onAvatarError($event, item.name)" />
        <div class="featured-card__overlay"></div>
        <div class="featured-card__content">
          <strong>{{ item.name }}</strong>
          <div class="featured-card__meta-row">
            <span class="featured-card__tag">{{ item.tags[0] }}</span>
            <span class="featured-card__count">{{ item.count }}聊过</span>
            <button type="button" class="btn-chat" @click.stop="$emit('chat', item.id)">聊一聊</button>
          </div>
        </div>
      </article>
    </div>

    <div v-else class="role-card-grid" ref="scrollContainer">
      <RoleCard
        v-for="role in roleItems"
        :key="role.id"
        :role="role"
        :variant="variant === 'soft' ? 'soft' : 'default'"
        @chat="$emit('chat', $event)"
        @detail="$emit('detail', $event)"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import type { FeaturedCardData, HomeRoleCardData } from './homeData'
import RoleCard from './RoleCard.vue'
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
      el.scrollBy({ left: 240, behavior: 'smooth' })
    }
  }
}

withDefaults(defineProps<{
  title: string
  variant?: 'visual' | 'role' | 'soft'
  featuredItems?: FeaturedCardData[]
  roleItems?: HomeRoleCardData[]
}>(), {
  variant: 'role',
  featuredItems: () => [],
  roleItems: () => [],
})

defineEmits<{
  chat: [id: number]
  detail: [id: number]
}>()
</script>

<style scoped lang="scss">
.home-section {
  display: grid;
  gap: 15px;
}

.featured-grid,
.role-card-grid {
  display: flex;
  overflow-x: auto;
  gap: 14px;
  padding-bottom: 8px;
  scroll-behavior: smooth;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}

.featured-card,
.role-card-grid > * {
  flex: 0 0 220px;
}

.featured-card {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  height: 100px;
  overflow: hidden;
  border-radius: 16px;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(75, 83, 150, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);

    .featured-card__bg {
      transform: scale(1.05);
    }
  }
}

.featured-card__bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.featured-card__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 40%, rgba(17, 17, 24, 0.8) 100%);
  z-index: 1;
}

.featured-card__content {
  position: relative;
  z-index: 2;
  padding: 12px;
  color: white;

  strong {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 6px;
  }
}

.featured-card__meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
}

.featured-card__tag {
  color: rgba(255, 255, 255, 0.9);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.featured-card__count {
  color: rgba(255, 255, 255, 0.7);
  flex: 1;
}

.btn-chat {
  border: 0;
  border-radius: 999px;
  padding: 4px 12px;
  color: #fff;
  background: rgba(255, 255, 255, 0.2);
  cursor: pointer;
  font-size: 11px;
  font-weight: 500;
  backdrop-filter: blur(4px);
  
  &:hover {
    background: rgba(107, 71, 245, 0.8);
  }
}

@media (max-width: 640px) {
  .featured-card,
  .role-card-grid > * {
    flex: 0 0 200px;
  }
}
</style>
