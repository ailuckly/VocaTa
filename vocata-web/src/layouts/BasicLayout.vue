<template>
  <div class="app-layout" :class="{ 'is-mobile': isMobileDevice, 'is-chat-route': isChatRoute }">
    <!-- 移动端遮罩 -->
    <div
      v-if="isMobileDevice && !isSidebarCollapsed"
      class="app-layout__overlay"
      @click="isSidebarCollapsed = true"
    />

    <AppSidebar :collapsed="isSidebarCollapsed" @toggle="handleToggleSidebar" />

    <main class="app-layout__main" data-test="app-main">
      <!-- 移动端顶栏 -->
      <div v-if="isMobileDevice" class="app-layout__topbar">
        <button type="button" class="app-layout__menu-btn" @click="handleToggleSidebar" aria-label="打开菜单">
          <el-icon><Expand /></el-icon>
        </button>
        <RouterLink to="/searchRole" class="app-layout__topbar-logo">
          <img src="@/assets/logo.svg" alt="VocaTa" />
          <span>VocaTa</span>
        </RouterLink>
      </div>

      <div class="app-layout__view" :class="{ 'is-chat-view': isChatRoute }">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/components/shell/AppSidebar.vue'
import { isMobile } from '@/utils/isMobile'

const isSidebarCollapsed = ref(false)
const router = useRouter()
const route = useRoute()
const isMobileDevice = isMobile()
const isChatRoute = route.path.startsWith('/chat/')

onMounted(() => {
  if (isMobileDevice) {
    isSidebarCollapsed.value = true
  }
})

const handleToggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

const handleExplore = () => {
  router.push('/searchRole')
}
</script>

<style lang="scss" scoped>
.app-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  height: 100vh;
  overflow: hidden;
  background: var(--vt-bg);
}

.app-layout__overlay {
  position: fixed;
  inset: 0;
  z-index: 39;
  background: oklch(0% 0 0 / 0.4);
}

.app-layout__main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: 100vh;
  overflow: hidden;
  background: var(--vt-bg);
}

.app-layout__view {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px 28px;
}

.app-layout__view.is-chat-view {
  overflow: hidden;
  padding: 0;
}

/* Mobile topbar */
.app-layout__topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--vt-line);
  flex-shrink: 0;
}

.app-layout__menu-btn {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: var(--vt-radius-sm);
  background: transparent;
  color: var(--vt-text);
  font-size: 20px;
  cursor: pointer;

  &:hover { background: var(--vt-surface-overlay); }
}

.app-layout__topbar-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;

  img { width: 22px; height: 22px; }

  span {
    font-size: 15px;
    font-weight: 700;
    background: linear-gradient(135deg, var(--vt-brand) 0%, oklch(65% 0.18 200) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

@media (max-width: 768px) {
  .app-layout {
    grid-template-columns: 1fr;
  }

  .app-layout__view {
    padding: 16px;
  }

  .app-layout.is-chat-route .app-layout__view {
    padding: 0;
  }
}
</style>
