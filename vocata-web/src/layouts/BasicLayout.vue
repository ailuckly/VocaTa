<template>
  <div class="app-layout" :class="{ 'is-mobile': isMobileDevice, 'is-chat-route': isChatRoute }">
    <AppSidebar :collapsed="isSidebarCollapsed" @toggle="handleToggleSidebar" />
    <main class="app-layout__main" data-test="app-main">
      <div v-if="isMobileDevice" class="app-layout__mobile-tools">
        <button type="button" @click="handleToggleSidebar">
          {{ isSidebarCollapsed ? '展开导航' : '收起导航' }}
        </button>
        <button type="button" @click="handleExplore">去探索</button>
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
  grid-template-columns: 260px minmax(0, 1fr);
  height: 100vh;
  overflow: hidden;
  background: var(--vt-bg);
}

.app-layout__main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: 100vh;
  padding: 24px 28px;
  overflow: hidden;
  background: var(--vt-bg);
}

.app-layout__view {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.app-layout__view.is-chat-view {
  min-height: 0;
  height: 100%;
}

.app-layout.is-chat-route .app-layout__main {
  padding: 0 28px;
}

.app-layout__mobile-tools {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.app-layout__mobile-tools button {
  border: 0;
  border-radius: 999px;
  padding: 10px 14px;
  background: var(--vt-surface);
  color: var(--vt-text);
}

@media (max-width: 768px) {
  .app-layout {
    grid-template-columns: 1fr;
  }

  .app-layout__main {
    height: 100vh;
    padding: 16px;
  }

  .app-layout.is-chat-route .app-layout__main {
    padding: 12px 16px;
  }
}
</style>
