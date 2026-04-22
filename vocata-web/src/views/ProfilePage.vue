<template>
  <div class="profile-page">
    <!-- 顶部用户信息 -->
    <div class="profile-page__hero">
      <div class="profile-page__avatar">
        <img v-if="userInfo.avatar" :src="userInfo.avatar" :alt="userInfo.nickname"
          @error="onAvatarError($event, userInfo.nickname)" />
        <span v-else>{{ userInfo.nickname.slice(0, 1) }}</span>
      </div>
      <div class="profile-page__info">
        <h1>{{ userInfo.nickname }}</h1>
        <p v-if="userInfo.email">{{ userInfo.email }}</p>
      </div>
    </div>

    <!-- 统计 -->
    <div class="profile-page__stats">
      <div class="profile-page__stat">
        <strong>{{ recentConversations.length }}</strong>
        <span>对话</span>
      </div>
      <div class="profile-page__stat">
        <strong>{{ favoriteRoles.length }}</strong>
        <span>收藏角色</span>
      </div>
    </div>

    <!-- 最近对话 -->
    <section v-if="recentConversations.length" class="profile-page__section">
      <h2>最近对话</h2>
      <div class="profile-page__list">
        <RouterLink
          v-for="item in recentConversations"
          :key="item.id"
          :to="`/chat/${item.id}`"
          class="profile-page__list-item"
        >
          <div class="profile-page__list-avatar">
            <img v-if="item.avatar" :src="item.avatar" :alt="item.title"
            @error="onAvatarError($event, item.title)" />
            <span v-else>{{ item.title.slice(0, 1) }}</span>
          </div>
          <div class="profile-page__list-body">
            <strong>{{ item.title }}</strong>
            <span>{{ item.subtitle }}</span>
          </div>
          <el-icon class="profile-page__list-arrow"><ArrowRight /></el-icon>
        </RouterLink>
      </div>
    </section>

    <!-- 收藏角色 -->
    <section v-if="favoriteRoles.length" class="profile-page__section">
      <h2>收藏角色</h2>
      <div class="profile-page__list">
        <div
          v-for="item in favoriteRoles"
          :key="item.id"
          class="profile-page__list-item"
        >
          <div class="profile-page__list-avatar">
            <img v-if="item.avatar" :src="item.avatar" :alt="item.title"
            @error="onAvatarError($event, item.title)" />
            <span v-else>{{ item.title.slice(0, 1) }}</span>
          </div>
          <div class="profile-page__list-body">
            <strong>{{ item.title }}</strong>
            <span>{{ item.subtitle }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { userApi } from '@/api/modules/user'
import { chatHistoryStore } from '@/store'
import { onAvatarError } from '@/utils/avatar'
import { computed, onMounted, ref } from 'vue'

const historyStore = chatHistoryStore()
const userInfo = ref({ nickname: '用户', avatar: '', email: '' })

const recentConversations = computed(() =>
  historyStore.chatHistory.slice(0, 6).map((item) => ({
    id: item.conversationUuid,
    title: item.characterName || item.title || '未命名对话',
    subtitle: item.lastMessageSummary || item.greeting || '继续上一次的对话',
    avatar: item.characterAvatarUrl || '',
  }))
)

const favoriteRoles = computed(() => {
  const seen = new Set<string>()
  return historyStore.chatHistory
    .filter((item) => {
      const key = item.characterId || item.characterName
      if (!key || seen.has(key)) return false
      seen.add(key)
      return true
    })
    .slice(0, 6)
    .map((item) => ({
      id: item.characterId || item.conversationUuid,
      title: item.characterName || item.title || '常聊角色',
      subtitle: item.greeting || item.lastMessageSummary || '',
      avatar: item.characterAvatarUrl || '',
    }))
})

onMounted(async () => {
  try {
    const [userRes] = await Promise.all([userApi.getUserInfo(), historyStore.getChatHistory()])
    if (userRes.code === 200 && userRes.data) {
      userInfo.value = {
        nickname: userRes.data.nickname || '用户',
        avatar: userRes.data.avatar || '',
        email: userRes.data.email || '',
      }
    }
  } catch {}
})
</script>

<style scoped lang="scss">
.profile-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 32px 0;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* Hero */
.profile-page__hero {
  display: flex;
  align-items: center;
  gap: 20px;
}

.profile-page__avatar {
  display: grid;
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  background: var(--vt-brand-soft);
  color: var(--vt-brand-strong);
  font-size: 28px;
  font-weight: 700;
  border: 2px solid var(--vt-line);

  img { width: 100%; height: 100%; object-fit: cover; }
}

.profile-page__info {
  h1 { margin: 0 0 4px; font-size: 22px; font-weight: 700; color: var(--vt-text); }
  p { margin: 0; font-size: 13px; color: var(--vt-text-muted); }
}

/* Stats */
.profile-page__stats {
  display: flex;
  gap: 24px;
}

.profile-page__stat {
  display: flex;
  flex-direction: column;
  gap: 2px;

  strong { font-size: 22px; font-weight: 700; color: var(--vt-text); }
  span { font-size: 12px; color: var(--vt-text-muted); }
}

/* Sections */
.profile-page__section {
  display: flex;
  flex-direction: column;
  gap: 8px;

  h2 {
    margin: 0;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.07em;
    text-transform: uppercase;
    color: var(--vt-text-muted);
    padding: 0 4px;
  }
}

.profile-page__list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.profile-page__list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--vt-radius-md);
  background: var(--vt-surface);
  border: 1px solid var(--vt-line);
  text-decoration: none;
  transition: background 0.12s;

  &:hover { background: var(--vt-surface-overlay); }
}

.profile-page__list-avatar {
  display: grid;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 10px;
  overflow: hidden;
  background: var(--vt-brand-soft);
  color: var(--vt-brand-strong);
  font-size: 14px;
  font-weight: 700;

  img { width: 100%; height: 100%; object-fit: cover; }
}

.profile-page__list-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;

  strong {
    font-size: 14px;
    font-weight: 500;
    color: var(--vt-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    font-size: 12px;
    color: var(--vt-text-muted);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.profile-page__list-arrow {
  color: var(--vt-text-muted);
  font-size: 14px;
  flex-shrink: 0;
}
</style>
