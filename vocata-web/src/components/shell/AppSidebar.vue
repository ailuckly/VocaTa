<template>
  <aside class="app-sidebar" data-test="app-sidebar" :class="{ 'is-hidden': collapsed && isMobileDevice }">
    <div class="app-sidebar__brand">
      <RouterLink class="app-sidebar__brand-link" to="/searchRole" aria-label="VocaTa">
        <img class="app-sidebar__logo-icon" src="@/assets/logo.svg" alt="" aria-hidden="true" />
        <span class="app-sidebar__logo-text">VocaTa</span>
      </RouterLink>
      <button
        v-if="isMobileDevice"
        type="button"
        class="app-sidebar__toggle"
        @click="$emit('toggle')"
        aria-label="关闭菜单"
      >
        <el-icon><Close /></el-icon>
      </button>
    </div>

    <AppSidebarProfile :avatar="userInfo.avatar" :display-name="userInfo.nickname" />

    <nav class="app-sidebar__nav" aria-label="主导航">
      <RouterLink to="/searchRole" class="app-sidebar__nav-item">
        <el-icon class="app-sidebar__nav-icon"><Compass /></el-icon>
        <span>探索</span>
      </RouterLink>
      <RouterLink to="/newRole" class="app-sidebar__nav-item">
        <el-icon class="app-sidebar__nav-icon"><EditPen /></el-icon>
        <span>创建角色</span>
      </RouterLink>
      <RouterLink to="/profile" class="app-sidebar__nav-item">
        <el-icon class="app-sidebar__nav-icon"><User /></el-icon>
        <span>我的空间</span>
      </RouterLink>
    </nav>

    <AppSidebarHistory
      :items="chatHistory"
      :active-conversation-uuid="activeConversationUuid"
      @open="openConversation"
    />

    <div class="app-sidebar__footer">
      <button class="app-sidebar__theme-btn" @click="toggleTheme" :title="isDark ? '切换浅色模式' : '切换深色模式'">
        <el-icon v-if="isDark"><Sunny /></el-icon>
        <el-icon v-else><Moon /></el-icon>
        <span>{{ isDark ? '浅色模式' : '深色模式' }}</span>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { userApi } from '@/api/modules/user'
import { chatHistoryStore } from '@/store'
import { isMobile } from '@/utils/isMobile'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
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
const { isDark, toggle: toggleTheme } = useTheme()

const userInfo = ref({
  nickname: 'VocaTa 用户',
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
        nickname: userRes.data.nickname || 'VocaTa 用户',
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
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 260px;
  min-width: 260px;
  max-width: 260px;
  flex: none;
  height: 100vh;
  padding: 20px 12px;
  background: var(--vt-surface);
  border-right: 1px solid var(--vt-line);
  overflow-y: auto;
  overflow-x: hidden;
}

/* Brand */
.app-sidebar__brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px 12px;
}

.app-sidebar__brand-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.app-sidebar__logo-icon {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.app-sidebar__logo-text {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.3px;
  background: linear-gradient(135deg, var(--vt-brand) 0%, oklch(65% 0.18 200) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.app-sidebar__toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: var(--vt-radius-sm);
  background: transparent;
  color: var(--vt-text-soft);
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: var(--vt-surface-overlay);
  }
}

/* Nav */
.app-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 4px 0;
}

.app-sidebar__nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--vt-radius-md);
  color: var(--vt-text-soft);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: background 0.15s, color 0.15s;

  &:hover {
    background: var(--vt-surface-overlay);
    color: var(--vt-text);
  }

  &.router-link-active {
    background: var(--vt-brand-soft);
    color: var(--vt-brand-strong);
  }
}

.app-sidebar__nav-icon {
  font-size: 16px;
  flex-shrink: 0;
}

/* Footer */
.app-sidebar__footer {
  margin-top: auto;
  padding-top: 8px;
  border-top: 1px solid var(--vt-line-subtle);
}

.app-sidebar__theme-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 12px;
  border: 0;
  border-radius: var(--vt-radius-md);
  background: transparent;
  color: var(--vt-text-soft);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;

  &:hover {
    background: var(--vt-surface-overlay);
    color: var(--vt-text);
  }

  .el-icon {
    font-size: 16px;
  }
}

@media (max-width: 768px) {
  .app-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 30;
    width: min(84vw, 280px);
    min-width: min(84vw, 280px);
    max-width: min(84vw, 280px);
    box-shadow: var(--vt-shadow-lg);
  }

  .app-sidebar.is-hidden {
    display: none;
  }
}
</style>
