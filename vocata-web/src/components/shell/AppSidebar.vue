<template>
  <aside class="app-sidebar" :class="{ 'is-hidden': collapsed && isMobileDevice, 'is-mini': collapsed && !isMobileDevice }">
    <div class="app-sidebar__top">
      <RouterLink class="app-sidebar__logo" to="/searchRole" aria-label="VocaTa 首页" :title="collapsed && !isMobileDevice ? 'VocaTa' : ''">
        <img src="@/assets/logo.svg" alt="" aria-hidden="true" class="app-sidebar__logo-icon" />
        <span class="app-sidebar__logo-text">VocaTa</span>
      </RouterLink>
      <button v-if="!isMobileDevice" class="app-sidebar__toggle" @click="$emit('toggle')" aria-label="收起/展开菜单" :title="collapsed ? '展开侧边栏' : '收起侧边栏'">
        <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
      </button>
    </div>

    <div class="app-sidebar__middle">
      <!-- 主导航 (类似 ChatGPT 的 New Chat / Explore) -->
      <nav class="app-sidebar__nav">
        <RouterLink to="/newRole" class="app-sidebar__nav-item" :title="collapsed && !isMobileDevice ? '创建角色' : ''">
          <el-icon><EditPen /></el-icon>
          <span>创建角色</span>
        </RouterLink>
        <RouterLink to="/searchRole" class="app-sidebar__nav-item" :title="collapsed && !isMobileDevice ? '探索' : ''">
          <el-icon><Compass /></el-icon>
          <span>探索广场</span>
        </RouterLink>
      </nav>

      <div class="app-sidebar__divider"></div>

      <!-- 对话历史 (极简纯文本风格) -->
      <div class="app-sidebar__history-header">
        <span>最近对话</span>
        <button type="button" @click="showComingSoon('最近对话')">更多 &gt;</button>
      </div>
      <div class="app-sidebar__history">
        <template v-if="chatHistory.length">
          <div
            v-for="item in chatHistory"
            :key="item.conversationUuid"
            class="app-sidebar__history-item-group"
            :class="{ 'is-active': item.conversationUuid === activeConversationUuid }"
          >
            <button
              class="app-sidebar__history-item"
              @click="openConversation(item.conversationUuid)"
              :title="item.characterName || item.title || '未命名对话'"
            >
              <div class="app-sidebar__history-avatar">
                <img v-if="item.characterAvatarUrl" :src="item.characterAvatarUrl" :alt="item.characterName" @error="onAvatarError($event, item.characterName || '?')" />
                <span v-else>{{ (item.characterName || item.title || '?').slice(0, 1) }}</span>
              </div>
              <span class="history-item-text">{{ item.characterName || item.title || '未命名对话' }}</span>
            </button>

            <!-- 操作菜单 (悬浮显示) -->
            <el-dropdown class="app-sidebar__history-actions" trigger="click" @command="(cmd) => handleHistoryCommand(cmd as string, item.conversationUuid)">
              <button class="history-action-btn" @click.stop><el-icon><MoreFilled /></el-icon></button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="delete" class="danger-item">
                    <el-icon><Delete /></el-icon>删除对话
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
        <p v-else class="app-sidebar__history-empty">还没有对话记录</p>
      </div>
    </div>

    <div class="app-sidebar__bottom">
      <!-- 底部只保留极其克制的用户入口 -->
      <div class="app-sidebar__footer">
        <button class="app-sidebar__user" @click="userMenuOpen = !userMenuOpen" :class="{ 'is-open': userMenuOpen }" :title="collapsed && !isMobileDevice ? userInfo.nickname : ''">
          <div class="app-sidebar__user-avatar">
            <img v-if="userInfo.avatar" :src="userInfo.avatar" :alt="userInfo.nickname"
              @error="onAvatarError($event, userInfo.nickname)" />
            <span v-else>{{ userInfo.nickname.slice(0, 1) }}</span>
          </div>
          <span class="app-sidebar__user-name">{{ userInfo.nickname }}</span>
        </button>

        <!-- 弹出菜单整合了原有底部所有零散按钮 -->
        <Transition name="user-menu">
          <div v-if="userMenuOpen" class="app-sidebar__user-menu">
            <button class="app-sidebar__user-menu-item" @click="showComingSoon('会员中心'); userMenuOpen = false">
              <el-icon><Star /></el-icon>
              <span>会员升级</span>
            </button>
            <button class="app-sidebar__user-menu-item" @click="showComingSoon('管理员入口'); userMenuOpen = false">
              <el-icon><Setting /></el-icon>
              <span>管理员入口</span>
            </button>
            <div class="app-sidebar__user-menu-divider"></div>
            <RouterLink to="/profile" class="app-sidebar__user-menu-item" @click="userMenuOpen = false">
              <el-icon><User /></el-icon>
              <span>个人资料</span>
            </RouterLink>
            <button class="app-sidebar__user-menu-item is-danger" @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </button>
          </div>
        </Transition>
      </div>
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
import { removeToken } from '@/utils/token'
import { onAvatarError } from '@/utils/avatar'
import {
  ArrowUp,
  Close,
  Compass,
  EditPen,
  Histogram,
  Setting,
  Star,
  SwitchButton,
  User,
  Fold,
  Expand,
  MoreFilled,
  Delete
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineProps<{ collapsed: boolean }>()
defineEmits<{ toggle: [] }>()

const router = useRouter()
const route = useRoute()
const isMobileDevice = isMobile()
const historyStore = chatHistoryStore()

const userInfo = ref({ nickname: '用户', avatar: '' })
const userMenuOpen = ref(false)
const chatHistory = computed(() => historyStore.chatHistory)
const activeConversationUuid = computed(() =>
  route.path.startsWith('/chat/') ? String(route.params.conversationUuid) : ''
)

const openConversation = (uuid: string) => router.push(`/chat/${uuid}`)
const showComingSoon = (name: string) => {
  ElMessage.info(`${name}即将开放`)
}

const handleHistoryCommand = async (command: string, uuid: string) => {
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定要删除这条对话吗？此操作不可恢复。', '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      })
      await historyStore.deleteChatHistory(uuid)
      if (activeConversationUuid.value === uuid) {
        router.push('/searchRole')
      }
    } catch {
      // User cancelled
    }
  }
}

const handleLogout = async () => {
  userMenuOpen.value = false
  try { await userApi.logout() } catch {}
  removeToken()
  router.push('/login')
}

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
  width: 260px;
  min-width: 260px;
  height: 100vh;
  position: sticky;
  top: 0;
  background: var(--vt-bg);
  border-right: 1px solid rgba(0, 0, 0, 0.04);
  overflow: hidden;
  padding: 16px 12px;
  transition: width 0.3s cubic-bezier(0.2, 0.8, 0.2, 1), min-width 0.3s cubic-bezier(0.2, 0.8, 0.2, 1), padding 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.app-sidebar.is-mini {
  width: 76px;
  min-width: 76px;
  padding: 20px 10px;
}

.app-sidebar__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  flex-shrink: 0;
  margin-bottom: 20px;
}

.app-sidebar__toggle {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 0;
  background: transparent;
  color: var(--vt-text-soft);
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
  
  &:hover {
    background: var(--vt-surface-overlay);
    color: var(--vt-text);
  }
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
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.3px;
  background: linear-gradient(135deg, var(--vt-brand) 0%, oklch(65% 0.18 200) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}


.app-sidebar__middle {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* Nav */
.app-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 10px 6px;
  flex-shrink: 0;
}

.app-sidebar__nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  border: 0;
  padding: 10px 14px;
  border-radius: 8px;
  color: #5c6275;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s;

  .el-icon { font-size: 16px; flex-shrink: 0; transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1); }

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%) scaleY(0);
    width: 4px;
    height: 18px;
    background: #7755ff;
    border-radius: 0 4px 4px 0;
    transition: transform 0.25s cubic-bezier(0.2, 0.8, 0.2, 1);
  }

  &:hover { background: #f7f8fa; color: #171b33; .el-icon { transform: scale(1.1); } }
  &.router-link-active {
    background: #f5f2ff;
    color: #7755ff;
    font-weight: 600;
    &::before {
      transform: translateY(-50%) scaleY(1);
    }
  }
}

/* Divider */
.app-sidebar__divider {
  height: 1px;
  background: var(--vt-line-subtle);
  margin: 12px 14px 4px;
  flex-shrink: 0;
}

/* History */
.app-sidebar__history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px 8px;
  flex-shrink: 0;

  span {
    font-size: 11px;
    font-weight: 600;
    color: var(--vt-text-muted);
  }

  button {
    border: 0;
    padding: 0;
    color: var(--vt-text-muted);
    background: transparent;
    cursor: pointer;
    font-size: 11px;
    font-weight: 600;
    
    &:hover { color: var(--vt-text); }
  }
}

.app-sidebar__history {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 4px 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px; /* ChatGPT style minimal gap */

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: transparent; border-radius: 2px; }
  &:hover::-webkit-scrollbar-thumb { background: var(--vt-line); }
}

.app-sidebar__history-item-group {
  position: relative;
  display: flex;
  align-items: center;
  border-radius: 8px;
  background: transparent;
  transition: background 0.15s;
}

.app-sidebar__history-item-group:hover {
  background: rgba(0,0,0,0.03);
}

.app-sidebar__history-item-group.is-active {
  background: rgba(0,0,0,0.06);
  .history-item-text { font-weight: 600; color: var(--vt-text); }
}

.app-sidebar__history-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  min-width: 0;
}

.app-sidebar__history-actions {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
  margin-right: 4px;
}

.app-sidebar__history-item-group:hover .app-sidebar__history-actions,
.app-sidebar__history-item-group.is-active .app-sidebar__history-actions {
  opacity: 1;
}

.history-action-btn {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--vt-text-soft);
  cursor: pointer;
  font-size: 14px;
}
.history-action-btn:hover {
  background: rgba(0,0,0,0.05);
  color: var(--vt-text);
}

.app-sidebar__history-avatar {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--vt-surface-overlay);
  color: var(--vt-text-soft);
  font-size: 11px;
  font-weight: bold;
  flex-shrink: 0;
  transition: all 0.3s;
}

.app-sidebar__history-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.history-item-text {
  font-size: 13px;
  color: var(--vt-text-soft);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.app-sidebar__bottom {
  margin-top: auto;
  display: grid;
  flex-shrink: 0;
  gap: 10px;
  padding: 8px 10px 10px;
}

.app-sidebar__history-empty {
  margin: 0;
  padding: 12px 10px;
  font-size: 12px;
  color: var(--vt-text-muted);
}

/* Footer */
.app-sidebar__footer {
  position: relative;
  display: grid;
  gap: 6px;
  padding: 0;
  flex-shrink: 0;
}

.app-sidebar__user {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.12s;

  &:hover, &.is-open { background: rgba(0,0,0,0.04); }
}

.app-sidebar__user-avatar {
  display: grid;
  width: 30px;
  height: 30px;
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
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--vt-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* User popup menu */
.app-sidebar__user-menu {
  position: absolute;
  bottom: calc(100% + 4px);
  left: 8px;
  right: 8px;
  background: var(--vt-surface);
  border: 1px solid var(--vt-line);
  border-radius: var(--vt-radius-md);
  box-shadow: var(--vt-shadow-md);
  overflow: hidden;
  z-index: 50;
}

.user-menu-enter-active, .user-menu-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}
.user-menu-enter-from, .user-menu-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.app-sidebar__user-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 14px;
  border: 0;
  background: transparent;
  color: var(--vt-text);
  font-size: 14px;
  text-decoration: none;
  cursor: pointer;
  transition: background 0.12s;

  .el-icon { font-size: 15px; color: var(--vt-text-soft); }

  &:hover { background: var(--vt-surface-overlay); }
  &.is-danger { color: var(--vt-danger); .el-icon { color: var(--vt-danger); } }
}

.app-sidebar__user-menu-divider {
  height: 1px;
  background: var(--vt-line-subtle);
  margin: 2px 0;
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

/* Mini Sidebar State Styles */
.app-sidebar.is-mini .app-sidebar__logo-text,
.app-sidebar.is-mini .app-sidebar__nav-item span,
.app-sidebar.is-mini .app-sidebar__history-header span,
.app-sidebar.is-mini .app-sidebar__history-header button,
.app-sidebar.is-mini .history-item-text,
.app-sidebar.is-mini .app-sidebar__user-name {
  display: none;
}

.app-sidebar.is-mini .app-sidebar__history-empty {
  font-size: 0;
  padding: 12px 0;
  text-align: center;
}
.app-sidebar.is-mini .app-sidebar__history-empty::after {
  content: '...';
  font-size: 12px;
}

.app-sidebar.is-mini .app-sidebar__top {
  flex-direction: column;
  gap: 16px;
  height: auto;
  margin-bottom: 24px;
}

.app-sidebar.is-mini .app-sidebar__logo {
  padding: 4px 0;
}

.app-sidebar.is-mini .app-sidebar__history-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  font-size: 13px;
}

.app-sidebar.is-mini .app-sidebar__history-item-group {
  justify-content: center;
}

.app-sidebar.is-mini .app-sidebar__history-actions {
  display: none;
}

.app-sidebar.is-mini .app-sidebar__nav-item,
.app-sidebar.is-mini .app-sidebar__history-item,
.app-sidebar.is-mini .app-sidebar__user {
  padding: 10px 0;
  justify-content: center;
}

.app-sidebar.is-mini .app-sidebar__history-header {
  padding: 12px 0;
  justify-content: center;
}
.app-sidebar.is-mini .app-sidebar__history-header::after {
  content: '';
  display: block;
  width: 24px;
  height: 2px;
  background: var(--vt-line-subtle);
  margin: 0 auto;
}

.app-sidebar.is-mini .app-sidebar__user-menu {
  left: 100%;
  margin-left: 8px;
  bottom: 0;
  width: 140px;
}

/* Mobile */
@media (max-width: 768px) {
  .app-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 40;
    width: min(82vw, 260px);
    min-width: unset;
    box-shadow: var(--vt-shadow-lg);
    transition: transform 0.25s ease;
  }

  .app-sidebar.is-hidden {
    transform: translateX(-100%);
  }
}
</style>
