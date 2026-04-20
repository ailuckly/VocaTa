<template>
  <section class="profile-page">
    <ProfileOverview
      data-test="profile-overview"
      :name="userInfo.nickname"
      subtitle="在这里延续最近的陪伴关系、角色偏好和聊天节奏。"
    />
    <ProfileRecentConversations data-test="profile-recents" :items="recentConversations" />
    <ProfileFavoriteRoles data-test="profile-favorites" :items="favoriteRoles" />
  </section>
</template>

<script setup lang="ts">
import { userApi } from '@/api/modules/user'
import { chatHistoryStore } from '@/store'
import ProfileFavoriteRoles from '@/components/profile/ProfileFavoriteRoles.vue'
import ProfileOverview from '@/components/profile/ProfileOverview.vue'
import ProfileRecentConversations from '@/components/profile/ProfileRecentConversations.vue'
import { computed, onMounted, ref } from 'vue'

const historyStore = chatHistoryStore()
const userInfo = ref({
  nickname: '语Ta 用户',
})

const recentConversations = computed(() =>
  historyStore.chatHistory.slice(0, 4).map((item) => ({
    id: item.conversationUuid,
    title: item.title || item.characterName || '未命名对话',
    subtitle: item.lastMessageSummary || item.greeting || '继续你上一次的对话。',
  })),
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
    .slice(0, 4)
    .map((item) => ({
      id: item.characterId || item.conversationUuid,
      title: item.characterName || item.title || '常聊角色',
      subtitle: item.greeting || item.lastMessageSummary || '继续一段熟悉的陪伴聊天。',
    }))
})

const loadProfileData = async () => {
  try {
    const [userRes] = await Promise.all([
      userApi.getUserInfo(),
      historyStore.getChatHistory(),
    ])

    if (userRes.code === 200 && userRes.data) {
      userInfo.value.nickname = userRes.data.nickname || '语Ta 用户'
    }
  } catch (error) {
    console.error('加载用户主页失败:', error)
  }
}

onMounted(() => {
  loadProfileData()
})
</script>

<style scoped lang="scss">
.profile-page {
  display: grid;
  gap: 24px;
  max-width: 1160px;
  margin: 0 auto;
  padding: 24px;
}
</style>
