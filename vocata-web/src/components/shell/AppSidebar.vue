<template>
  <aside class="app-sidebar" data-test="app-sidebar" :class="{ 'is-hidden': collapsed && isMobileDevice }">
    <div class="app-sidebar__brand">
      <RouterLink class="app-sidebar__brand-link" to="/searchRole" aria-label="语Ta">
        <img class="app-sidebar__logo" src="@/assets/images/logo-text.png" alt="语Ta" />
      </RouterLink>
      <button
        v-if="isMobileDevice"
        type="button"
        class="app-sidebar__toggle"
        @click="$emit('toggle')"
      >
        关闭
      </button>
    </div>

    <AppSidebarProfile :avatar="userInfo.avatar" :display-name="userInfo.nickname" />

    <nav class="app-sidebar__nav" aria-label="主导航">
      <RouterLink to="/searchRole">探索</RouterLink>
      <RouterLink to="/newRole">创建角色</RouterLink>
      <RouterLink to="/profile">我的空间</RouterLink>
    </nav>

    <AppSidebarHistory
      :items="chatHistory"
      :active-conversation-uuid="activeConversationUuid"
      @open="openConversation"
    />
  </aside>
</template>

<script setup lang="ts">
import { userApi } from '@/api/modules/user'
import { chatHistoryStore } from '@/store'
import { isMobile } from '@/utils/isMobile'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppSidebarHistory from './AppSidebarHistory.vue'
import AppSidebarProfile from './AppSidebarProfile.vue'

defineProps<{
  collapsed: boolean
}>()

defineEmits<{
  toggle: []
}>()

const router = useRouter()
const route = useRoute()
const isMobileDevice = isMobile()
const historyStore = chatHistoryStore()

const userInfo = ref({
  nickname: '语Ta 用户',
  avatar: '',
})

const chatHistory = computed(() => historyStore.chatHistory.slice(0, 8))
const activeConversationUuid = computed(() => {
  if (route.path.startsWith('/chat/') && route.params.conversationUuid) {
    return String(route.params.conversationUuid)
  }
  return ''
})

const openConversation = (conversationUuid: string) => {
  router.push(`/chat/${conversationUuid}`)
}

const loadSidebarData = async () => {
  try {
    const [userRes] = await Promise.all([
      userApi.getUserInfo(),
      historyStore.getChatHistory(),
    ])

    if (userRes.code === 200 && userRes.data) {
      userInfo.value = {
        nickname: userRes.data.nickname || '语Ta 用户',
        avatar: userRes.data.avatar || '',
      }
    }
  } catch (error) {
    console.error('加载侧边栏数据失败:', error)
  }
}

onMounted(() => {
  loadSidebarData()
})
</script>

<style scoped lang="scss">
.app-sidebar {
  display: grid;
  align-content: start;
  gap: 16px;
  width: 288px;
  min-width: 288px;
  max-width: 288px;
  flex: none;
  height: 100vh;
  padding: 24px 20px;
  background: color-mix(in srgb, var(--vt-bg) 82%, white);
  border-right: 1px solid var(--vt-line);
  overflow-y: auto;
  overflow-x: hidden;
}

.app-sidebar__brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.app-sidebar__brand-link {
  display: flex;
  align-items: center;
  min-width: 0;
}

.app-sidebar__logo {
  display: block;
  width: 128px;
  height: auto;
}

.app-sidebar__toggle {
  border: 0;
  border-radius: 999px;
  padding: 8px 12px;
  background: var(--vt-surface);
  color: var(--vt-text-soft);
  cursor: pointer;
}

.app-sidebar__nav {
  display: grid;
  gap: 6px;
  padding: 8px;
  border-radius: 20px;
  background: color-mix(in srgb, var(--vt-surface) 76%, var(--vt-brand) 8%);
}

.app-sidebar__nav a {
  display: block;
  padding: 10px 12px;
  border-radius: 14px;
  color: var(--vt-text-soft);
  font-size: 14px;
  line-height: 1.3;
  font-weight: 500;
}

.app-sidebar__nav a.router-link-active {
  background: var(--vt-surface);
  color: var(--vt-text);
  box-shadow: var(--vt-shadow);
}

@media (max-width: 768px) {
  .app-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 30;
    width: min(84vw, 320px);
    min-width: min(84vw, 320px);
    max-width: min(84vw, 320px);
    box-shadow: 24px 0 48px rgba(19, 43, 42, 0.18);
  }

  .app-sidebar.is-hidden {
    display: none;
  }
}
</style>
