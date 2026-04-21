<template>
  <aside class="app-sidebar" :class="{ 'is-hidden': collapsed && isMobileDevice }">
    <!-- 顶部：Logo + 新建按钮 -->
    <div class="app-sidebar__top">
      <RouterLink class="app-sidebar__logo" to="/searchRole" aria-label="VocaTa 首页">
        <img src="@/assets/logo.svg" alt="" aria-hidden="true" class="app-sidebar__logo-icon" />
        <span class="app-sidebar__logo-text">VocaTa</span>
      </RouterLink>
      <RouterLink to="/newRole" class="app-sidebar__new-btn" title="创建角色">
        <el-icon><Plus /></el-icon>
      </RouterLink>
    </div>

    <!-- 主导航 -->
    <nav class="app-sidebar__nav">
      <RouterLink to="/searchRole" class="app-sidebar__nav-item">
        <el-icon><Compass /></el-icon>
        <span>探索</span>
      </RouterLink>
      <RouterLink to="/newRole" class="app-sidebar__nav-item">
        <el-icon><EditPen /></el-icon>
        <span>创建角色</span>
      </RouterLink>
      <RouterLink to="/profile" class="app-sidebar__nav-item">
        <el-icon><User /></el-icon>
        <span>我的空间</span>
      </RouterLink>
    </nav>

    <div class="app-sidebar__divider"></div>

    <!-- 对话历史 -->
    <div class="app-sidebar__history-header">
      <span>最近对话</span>
    </div>
    <div class="app-sidebar__history">
      <template v-if="chatHistory.length">
        <button
          v-for="item in chatHistory"
          :key="item.conversationUuid"
          class="app-sidebar__history-item"
          :class="{ 'is-active': item.conversationUuid === activeConversationUuid }"
          @click="openConversation(item.conversationUuid)"
        >
          <div class="app-sidebar__history-avatar">
            <img v-if="item.characterAvatarUrl" :src="item.characterAvatarUrl" :alt="item.characterName || ''" />
            <span v-else>{{ (item.characterName || '?').slice(0, 1) }}</span>
          </div>
          <div class="app-sidebar__history-body">
            <strong>{{ item.characterName || item.title || '未命名对话' }}</strong>
            <span>{{ item.lastMessageSummary || item.greeting || '继续上一次对话' }}</span>
          </div>
        </button>
      </template>
      <p v-else class="app-sidebar__history-empty">还没有对话记录</p>
    </div>

    <!-- 底部：用户 + 主题 -->
    <div class="app-sidebar__footer">
      <RouterLink to="/profile" class="app-sidebar__user">
        <div class="app-sidebar__user-avatar">
          <img v-if="userInfo.avatar" :src="userInfo.avatar" :alt="userInfo.nickname" />
          <span v-else>{{ userInfo.nickname.slice(0, 1) }}</span>
        </div>
        <span class="app-sidebar__user-name">{{ userInfo.nickname }}</span>
      </RouterLink>
      <button class="app-sidebar__theme-btn" @click="toggleTheme" :title="isDark ? '浅色模式' : '深色模式'">
        <el-icon v-if="isDark"><Sunny /></el-icon>
        <el-icon v-else><Moon /></el-icon>
      </button>
    </div>

    <!-- 移动端关闭按钮 -->
    <button v-if="isMobileDevice" class="app-sidebar__close" @click="$emit('toggle')" aria-label="关闭菜单">
      <el-icon><Close /></el-icon>
    </button>
  </aside>
</template>

<script setup lang="ts">
import { userApi } from '@/api/modules/user'
import { chatHistoryStore } from '@/store'
import { isMobile } from '@/utils/isMobile'
import { useTheme } from '@/composables/useTheme'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineProps<{ collapsed: boolean }>()
defineEmits<{ toggle: [] }>()

const router = useRouter()
const route = useRoute()
const isMobileDevice = isMobile()
const historyStore = chatHistoryStore()
const { isDark, toggle: toggleTheme } = useTheme()

const userInfo = ref({ nickname: '用户', avatar: '' })
const chatHistory = computed(() => historyStore.chatHistory.slice(0, 12))
const activeConversationUuid = computed(() =>
  route.path.startsWith('/chat/') ? String(route.params.conversationUuid) : ''
)

const openConversation = (uuid: string) => router.push(`/chat/${uuid}`)

onMounted(async () => {
  try {
    const [userRes] = await Promise.all([userApi.getUserInfo(), historyStore.getChatHistory()])
    if (userRes.code === 200 && userRes.data) {
      userInfo.value = { nickname: userRes.data.nickname || '用户', avatar: userRes.data.avatar || '' }
    }
  } catch {}
})
</script>

<style scoped lang="scss">
.app-sidebar {
  display: flex;
  flex-direction: column;
  width: 280px;
  min-width: 280px;
  height: 100vh;
  background: var(--vt-surface);
  border-right: 1px solid var(--vt-line);
  overflow: hidden;
}

/* Top */
.app-sidebar__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 8px;
  flex-shrink: 0;
}

.app-sidebar__logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.app-sidebar__logo-icon {
  width: 26px;
  height: 26px;
}

.app-sidebar__logo-text {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.3px;
  background: linear-gradient(135deg, var(--vt-brand) 0%, oklch(65% 0.18 200) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.app-sidebar__new-btn {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: var(--vt-radius-sm);
  background: var(--vt-brand-soft);
  color: var(--vt-brand-strong);
  font-size: 16px;
  text-decoration: none;
  transition: background 0.15s;

  &:hover { background: var(--vt-line); }
}

/* Nav */
.app-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 4px 8px;
  flex-shrink: 0;
}

.app-sidebar__nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: var(--vt-radius-sm);
  color: var(--vt-text-soft);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: background 0.12s, color 0.12s;

  .el-icon { font-size: 16px; flex-shrink: 0; }

  &:hover { background: var(--vt-surface-overlay); color: var(--vt-text); }
  &.router-link-active { background: var(--vt-brand-soft); color: var(--vt-brand-strong); }
}

/* Divider */
.app-sidebar__divider {
  height: 1px;
  background: var(--vt-line-subtle);
  margin: 4px 16px;
  flex-shrink: 0;
}

/* History */
.app-sidebar__history-header {
  padding: 8px 18px 4px;
  flex-shrink: 0;

  span {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: var(--vt-text-muted);
  }
}

.app-sidebar__history {
  flex: 1;
  overflow-y: auto;
  padding: 2px 8px;
  display: flex;
  flex-direction: column;
  gap: 1px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--vt-line); border-radius: 2px; }
}

.app-sidebar__history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 7px 10px;
  border: 0;
  border-radius: var(--vt-radius-sm);
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.12s;
  min-width: 0;

  &:hover { background: var(--vt-surface-overlay); }

  &.is-active {
    background: var(--vt-brand-soft);

    strong { color: var(--vt-brand-strong); }
  }
}

.app-sidebar__history-avatar {
  display: grid;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 8px;
  overflow: hidden;
  background: var(--vt-surface-overlay);
  color: var(--vt-text-soft);
  font-size: 12px;
  font-weight: 700;

  img { width: 100%; height: 100%; object-fit: cover; }
}

.app-sidebar__history-body {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;

  strong, span {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong { font-size: 13px; font-weight: 500; color: var(--vt-text); }
  span { font-size: 11px; color: var(--vt-text-muted); }
}

.app-sidebar__history-empty {
  margin: 0;
  padding: 12px 10px;
  font-size: 12px;
  color: var(--vt-text-muted);
}

/* Footer */
.app-sidebar__footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid var(--vt-line-subtle);
  flex-shrink: 0;
}

.app-sidebar__user {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  padding: 6px 8px;
  border-radius: var(--vt-radius-sm);
  text-decoration: none;
  transition: background 0.12s;

  &:hover { background: var(--vt-surface-overlay); }
}

.app-sidebar__user-avatar {
  display: grid;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  background: var(--vt-brand-soft);
  color: var(--vt-brand-strong);
  font-size: 12px;
  font-weight: 700;

  img { width: 100%; height: 100%; object-fit: cover; }
}

.app-sidebar__user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--vt-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-sidebar__theme-btn {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border: 0;
  border-radius: var(--vt-radius-sm);
  background: transparent;
  color: var(--vt-text-muted);
  font-size: 16px;
  cursor: pointer;
  transition: background 0.12s, color 0.12s;

  &:hover { background: var(--vt-surface-overlay); color: var(--vt-text); }
}

/* Mobile close */
.app-sidebar__close {
  position: absolute;
  top: 14px;
  right: 14px;
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: var(--vt-radius-sm);
  background: var(--vt-surface-overlay);
  color: var(--vt-text-soft);
  cursor: pointer;
}

/* Mobile */
@media (max-width: 768px) {
  .app-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 40;
    width: min(82vw, 280px);
    min-width: unset;
    box-shadow: var(--vt-shadow-lg);
    transition: transform 0.25s ease;
  }

  .app-sidebar.is-hidden {
    transform: translateX(-100%);
  }
}
</style>
