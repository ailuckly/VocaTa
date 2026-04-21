<template>
  <div class="settings-page">
    <h1 class="settings-page__title">设置</h1>

    <section class="settings-page__section">
      <h2>外观</h2>
      <div class="settings-page__item">
        <div class="settings-page__item-info">
          <strong>主题</strong>
          <span>切换深色或浅色界面</span>
        </div>
        <div class="settings-page__theme-toggle">
          <button
            class="settings-page__theme-btn"
            :class="{ 'is-active': !isDark }"
            @click="isDark && toggleTheme()"
          >
            <el-icon><Sunny /></el-icon>
            浅色
          </button>
          <button
            class="settings-page__theme-btn"
            :class="{ 'is-active': isDark }"
            @click="!isDark && toggleTheme()"
          >
            <el-icon><Moon /></el-icon>
            深色
          </button>
        </div>
      </div>
    </section>

    <section class="settings-page__section">
      <h2>账号</h2>
      <button class="settings-page__item is-btn" @click="handleLogout">
        <div class="settings-page__item-info">
          <strong class="is-danger">退出登录</strong>
          <span>退出当前账号</span>
        </div>
        <el-icon><ArrowRight /></el-icon>
      </button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { userApi } from '@/api/modules/user'
import { useTheme } from '@/composables/useTheme'
import { removeToken } from '@/utils/token'
import { useRouter } from 'vue-router'

const router = useRouter()
const { isDark, toggle: toggleTheme } = useTheme()

const handleLogout = async () => {
  try { await userApi.logout() } catch {}
  removeToken()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.settings-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 32px 0;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.settings-page__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--vt-text);
}

.settings-page__section {
  display: flex;
  flex-direction: column;
  gap: 2px;

  h2 {
    margin: 0 0 8px;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.07em;
    text-transform: uppercase;
    color: var(--vt-text-muted);
    padding: 0 4px;
  }
}

.settings-page__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: var(--vt-radius-md);
  background: var(--vt-surface);
  border: 1px solid var(--vt-line);

  &.is-btn {
    width: 100%;
    border: 0;
    cursor: pointer;
    text-align: left;
    transition: background 0.12s;

    &:hover { background: var(--vt-surface-overlay); }

    .el-icon { color: var(--vt-text-muted); }
  }
}

.settings-page__item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;

  strong {
    font-size: 14px;
    font-weight: 500;
    color: var(--vt-text);

    &.is-danger { color: var(--vt-danger); }
  }

  span {
    font-size: 12px;
    color: var(--vt-text-muted);
  }
}

.settings-page__theme-toggle {
  display: flex;
  gap: 4px;
  background: var(--vt-surface-overlay);
  padding: 3px;
  border-radius: var(--vt-radius-sm);
}

.settings-page__theme-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--vt-text-soft);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.12s, color 0.12s;

  .el-icon { font-size: 14px; }

  &.is-active {
    background: var(--vt-surface);
    color: var(--vt-text);
    box-shadow: var(--vt-shadow-sm);
  }
}
</style>
