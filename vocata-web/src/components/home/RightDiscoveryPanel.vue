<template>
  <aside class="right-discovery" aria-label="发现推荐">
    <section class="rail-card">
      <div class="rail-card__heading">
        <h2>热门标签</h2>
        <button type="button">更多 &gt;</button>
      </div>
      <div class="tag-cloud">
        <button v-for="tag in tags" :key="tag" type="button" @click="$emit('tag', tag)"># {{ tag }}</button>
      </div>
    </section>

    <section class="rail-card">
      <div class="rail-card__heading">
        <h2>剧情主题</h2>
        <button type="button">更多 &gt;</button>
      </div>
      <div class="topic-list">
        <article v-for="topic in topics" :key="topic.title">
          <img :src="topic.cover" :alt="topic.title" @error="onAvatarError($event, topic.title)" />
          <div>
            <strong>{{ topic.title }}</strong>
            <span>{{ topic.count }} 人聊过</span>
          </div>
        </article>
      </div>
    </section>

    <section class="rail-card">
      <div class="rail-card__heading">
        <h2>创作者推荐</h2>
        <button type="button">更多 &gt;</button>
      </div>
      <CreatorRecommendList :creators="creators" :followed-names="followedNames" @follow="$emit('follow', $event)" />
    </section>

    <section class="rail-card rail-card--member">
      <strong>VocaTa 会员</strong>
      <span>专属记忆 · 更长回复 · 优先体验</span>
      <button type="button" @click="$emit('member')">了解更多</button>
    </section>
  </aside>
</template>

<script setup lang="ts">
import CreatorRecommendList from './CreatorRecommendList.vue'
import type { CreatorData, TopicData } from './homeData'
import { onAvatarError } from '@/utils/avatar'

defineProps<{
  tags: string[]
  topics: TopicData[]
  creators: CreatorData[]
  followedNames: string[]
}>()

defineEmits<{
  tag: [tag: string]
  follow: [name: string]
  member: []
}>()
</script>

<style scoped lang="scss">
.right-discovery {
  position: sticky;
  top: 24px;
  display: grid;
  align-content: start;
  gap: 16px;
  max-height: calc(100vh - 96px);
  overflow: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar { display: none; }
}

.rail-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(129, 140, 248, 0.15);
  box-shadow: 0 12px 30px rgba(73, 80, 150, 0.04);
}

.rail-card__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;

  h2 {
    margin: 0;
    color: #171b33;
    font-size: 16px;
    font-weight: 700;
  }

  button {
    border: 0;
    color: #a0a5b5;
    background: transparent;
    cursor: pointer;
    font-size: 13px;
    font-weight: 500;
    transition: color 0.2s;
    
    &:hover {
      color: #7755ff;
    }
  }
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  button {
    border: 0;
    border-radius: 999px;
    padding: 6px 14px;
    color: #6b47f5;
    background: #f0ecff;
    cursor: pointer;
    font-size: 13px;
    font-weight: 500;
    transition: all 0.2s;

    &:hover {
      background: #e3dbff;
    }
  }
}

.topic-list {
  display: grid;
  gap: 14px;
}

.topic-list article {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 58px;
}

.topic-list img {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  object-fit: cover;
}

.topic-list div {
  min-width: 0;
}

.topic-list strong,
.topic-list span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-list strong {
  color: #171b33;
  font-size: 13px;
  font-weight: 800;
}

.topic-list span {
  margin-top: 4px;
  color: #737894;
  font-size: 12px;
}

.rail-card--member {
  gap: 8px;
  background: #fdfbf7;
  border: 1px solid rgba(212, 175, 55, 0.2);
  position: relative;
  overflow: hidden;

  strong {
    color: #b6782b;
    font-size: 15px;
    font-weight: 700;
  }

  span {
    color: #a0a5b5;
    font-size: 12px;
  }

  button {
    margin-top: 4px;
    align-self: flex-start;
    border: 0;
    border-radius: 999px;
    padding: 6px 16px;
    color: #b6782b;
    background: transparent;
    border: 1px solid rgba(212, 175, 55, 0.4);
    cursor: pointer;
    font-size: 13px;
    font-weight: 600;
    transition: all 0.2s;
    
    &:hover {
      background: rgba(212, 175, 55, 0.1);
    }
  }
}

@media (max-width: 1280px) {
  .right-discovery {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    max-height: none;
  }

  .rail-card--member {
    grid-column: 1 / -1;
  }
}

@media (max-width: 820px) {
  .right-discovery {
    grid-template-columns: 1fr;
  }
}
</style>
