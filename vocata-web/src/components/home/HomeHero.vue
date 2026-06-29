<template>
  <section class="hero-card">
    <div class="hero-bg" :style="bgStyle"></div>
      <div class="hero-bg__overlay"></div>

      <div class="hero-flush-right">
        <img :src="role.avatarUrl || fallbackCover" class="portrait-img" alt="" @error="onAvatarError($event)" />
        <div class="portrait-overlay">
          <div class="portrait-name-tag">
            <div class="tag-line left"></div>
            <span class="tag-text">{{ role.name || '未命名' }}</span>
            <div class="tag-line right"></div>
          </div>
          <div class="portrait-quote">
            <span>{{ role.greeting || role.description || '“快来和我聊天吧！”' }}</span>
          </div>
        </div>
      </div>

      <div class="hero-content">
        <div class="hero-content__left">
          <h1>{{ role.name || '未命名角色' }}</h1>
          <p>{{ description }}</p>
          <button class="btn-chat" @click="$emit('chat', role.id)">立即体验</button>
          
          <div class="hero-thumbnails">
            <button class="nav-arrow" @click="prevRole">&lt;</button>
            <div class="thumb-list">
              <img 
                v-for="(item, index) in switchRoles" 
                :key="item.id" 
                :src="item.avatarUrl || fallbackCover" 
                :class="{ 'is-active': index === activeIndex }"
                @click="$emit('select', index)"
                @error="onAvatarError($event)"
              />
            </div>
            <button class="nav-arrow" @click="nextRole">&gt;</button>
          </div>
        </div>
      </div>
    </section>
</template>

<script setup lang="ts">
import type { roleInfo } from '@/types/common'
import { computed } from 'vue'

const fallbackCover = '/avatars/harry.png'

const props = defineProps<{
  badge: string
  role: roleInfo
  tags: string[]
  description: string
  switchRoles: roleInfo[]
  activeIndex: number
}>()

const emit = defineEmits<{
  chat: [characterId: number]
  detail: [role: roleInfo]
  select: [index: number]
}>()

const bgStyle = computed(() => {
  const imageUrl = props.role.avatarUrl || fallbackCover
  return {
    backgroundImage: `url(${imageUrl})`
  }
})

const prevRole = () => {
  const index = props.activeIndex > 0 ? props.activeIndex - 1 : props.switchRoles.length - 1
  emit('select', index)
}

const nextRole = () => {
  const index = props.activeIndex < props.switchRoles.length - 1 ? props.activeIndex + 1 : 0
  emit('select', index)
}

const onAvatarError = (e: Event) => {
  const target = e.target as HTMLImageElement
  target.src = fallbackCover
}
</script>

<style scoped lang="scss">
.hero-card {
  position: relative;
  height: 320px;
  border-radius: 24px;
  overflow: hidden;
  margin-bottom: 24px;
}

.hero-bg {
  position: absolute;
  inset: 0;
  border-radius: 32px;
  overflow: hidden;
  background: #2a2b30;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08);
}

.hero-bg::before {
  content: '';
  position: absolute;
  inset: -20px;
  background-image: inherit;
  background-size: cover;
  background-position: center;
  filter: blur(24px) brightness(0.6);
  z-index: 1;
}

.hero-bg__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(40, 42, 48, 0.95) 0%, rgba(40, 42, 48, 0.7) 45%, rgba(40, 42, 48, 0.2) 100%);
  z-index: 2;
  border-radius: 24px;
}

.hero-content {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 32px 40px;
}

.hero-content__left {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 500px;
}

.hero-content__left h1 {
  font-size: 36px;
  font-weight: 800;
  color: white;
  margin: 0 0 12px;
  letter-spacing: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-content__left p {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.6;
  margin: 0 0 24px;
  height: 48px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.btn-chat {
  align-self: flex-start;
  padding: 12px 36px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  color: #171b33;
  font-weight: 800;
  font-size: 15px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #ffffff;
    transform: scale(1.02);
  }
}

.hero-thumbnails {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: auto;
}

.nav-arrow {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: transparent;
  color: white;
  font-size: 14px;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.2);
  }
}

.thumb-list {
  display: flex;
  gap: 12px;
}

.thumb-list img {
  width: 48px;
  height: 64px;
  border-radius: 12px;
  object-fit: cover;
  opacity: 0.5;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;

  &:hover {
    opacity: 0.8;
  }

  &.is-active {
    opacity: 1;
    border-color: #a48cff;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(164, 140, 255, 0.3);
  }
}

.hero-flush-right {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 50%;
  max-width: 480px;
  z-index: 2;
  -webkit-mask-image: linear-gradient(to right, transparent, black 30%);
  mask-image: linear-gradient(to right, transparent, black 30%);
}

.portrait-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.portrait-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 40%, rgba(10, 10, 15, 0.9) 100%);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
  padding: 20px 16px;
}

.portrait-name-tag {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  width: 100%;
}

.tag-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.8));
  &.right {
    background: linear-gradient(270deg, transparent, rgba(255,255,255,0.8));
  }
}

.tag-text {
  font-size: 16px;
  font-weight: 800;
  color: white;
  padding: 4px 16px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.2);
}

.portrait-quote {
  text-align: center;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

@media (max-width: 960px) {
  .hero-content {
    padding: 24px;
  }
  .hero-flush-right {
    width: 40%;
  }
  .hero-content__left h1 {
    font-size: 24px;
  }
}

@media (max-width: 640px) {
  .hero-flush-right {
    display: none;
  }
}
</style>
