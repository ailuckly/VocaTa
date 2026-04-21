<template>
  <div class="discovery">
    <!-- ── 页面标题 ──────────────────────────────────────── -->
    <div class="discovery__header">
      <h1 class="discovery__title">探索角色</h1>
      <p class="discovery__subtitle">找到今天适合开口的陪伴对象，立刻进入对话</p>
    </div>

    <!-- ── Hero Banner（含角色切换） ─────────────────────── -->
    <section class="hero" v-if="heroRole">
      <div class="hero__main">
        <!-- 左：文案 -->
        <div class="hero__copy">
          <span class="hero__badge">精选推荐</span>
          <h2 class="hero__name">{{ heroRole.name }}</h2>
          <p class="hero__desc">{{ heroRole.greeting || heroRole.description || '开始一段真实的陪伴对话' }}</p>
          <div class="hero__actions">
            <button class="hero__cta" @click="startConversation(heroRole.id)">立即体验</button>
            <span class="hero__meta" v-if="heroRole.chatCount">{{ heroRole.chatCount.toLocaleString() }} 次对话</span>
          </div>
        </div>
        <!-- 右：角色图 -->
        <div class="hero__visual">
          <Transition name="hero-img">
            <img
              :key="heroRole.id"
              :src="heroRole.avatarUrl"
              :alt="heroRole.name || ''"
              @error="onAvatarError($event, heroRole.name || '?')"
            />
          </Transition>
          <div class="hero__visual-fade"></div>
        </div>
      </div>

      <!-- 角色切换区（Banner 内部底部） -->
      <div class="hero__switcher" v-if="featuredRoles.length > 1">
        <button
          class="hero__switcher-arrow"
          @click="switchHero(-1)"
          :disabled="heroIndex <= 0"
          aria-label="上一个"
        >‹</button>
        <div class="hero__switcher-track">
          <button
            v-for="(role, i) in featuredRoles"
            :key="role.id"
            class="hero__switcher-item"
            :class="{ 'is-active': heroIndex === i }"
            @click="heroIndex = i"
          >
            <img
              :src="role.avatarUrl"
              :alt="role.name || ''"
              @error="onAvatarError($event, role.name || '?')"
            />
          </button>
        </div>
        <button
          class="hero__switcher-arrow"
          @click="switchHero(1)"
          :disabled="heroIndex >= featuredRoles.length - 1"
          aria-label="下一个"
        >›</button>
      </div>
    </section>

    <!-- ── 分类 Tabs ─────────────────────────────────────── -->
    <nav class="category-tabs">
      <button
        v-for="tab in TABS"
        :key="tab.value"
        class="category-tabs__item"
        :class="{ 'is-active': activeTab === tab.value }"
        @click="selectTab(tab.value)"
      >{{ tab.label }}</button>
    </nav>

    <!-- ── 内容卡片网格 ──────────────────────────────────── -->
    <section class="content-grid">
      <div
        v-for="role in roleList"
        :key="role.id"
        class="content-card"
        @click="startConversation(role.id)"
      >
        <div class="content-card__media">
          <img
            :src="role.avatarUrl"
            :alt="role.name || ''"
            @error="onAvatarError($event, role.name || '?')"
          />
          <div class="content-card__gradient"></div>
          <div class="content-card__label">
            <strong>{{ role.name }}</strong>
            <span v-if="role.chatCount">{{ role.chatCount.toLocaleString() }} 次对话</span>
          </div>
        </div>
        <p class="content-card__desc">{{ role.greeting || role.description || '开始一段对话' }}</p>
      </div>
    </section>

    <!-- 加载更多 -->
    <div v-if="hasMore" class="discovery__more">
      <button @click="loadMore" :disabled="loading">{{ loading ? '加载中…' : '加载更多' }}</button>
    </div>

    <RoleDialog :item="roleSelected" v-if="infoShow && roleSelected" @close="infoShow = false" />
  </div>
</template>

<script setup lang="ts">
import { roleApi } from '@/api/modules/role'
import { chatHistoryStore } from '@/store'
import type { roleInfo } from '@/types/common'
import { onAvatarError } from '@/utils/avatar'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import RoleDialog from './components/RoleDialog.vue'

const router = useRouter()

const TABS = [
  { label: '推荐', value: '' },
  { label: '女', value: '女' },
  { label: '男', value: '男' },
  { label: '奇幻', value: '奇幻' },
  { label: '游戏', value: '游戏' },
  { label: '动漫', value: '动漫' },
  { label: '影视', value: '影视' },
  { label: '历史', value: '历史' },
  { label: '科幻', value: '科幻' },
]

// ── 精选 & Hero ──────────────────────────────────────────
const featuredRoles = ref<roleInfo[]>([])
const heroIndex = ref(0)
const heroRole = computed(() => featuredRoles.value[heroIndex.value] ?? null)

const switchHero = (dir: number) => {
  const next = heroIndex.value + dir
  if (next >= 0 && next < featuredRoles.value.length) heroIndex.value = next
}

// ── 角色列表 ─────────────────────────────────────────────
const roleList = ref<roleInfo[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 15
const loading = ref(false)
const hasMore = computed(() => roleList.value.length < total.value)
const activeTab = ref('')

const getRoleList = async (reset = false) => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await roleApi.getPublicRoleList({
      pageNum: pageNum.value,
      pageSize,
      orderDirection: 'desc',
      tags: activeTab.value ? [activeTab.value] : undefined,
    })
    roleList.value = reset ? res.data.list : [...roleList.value, ...res.data.list]
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const selectTab = async (tab: string) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  pageNum.value = 1
  await getRoleList(true)
}

const loadMore = async () => {
  pageNum.value++
  await getRoleList(false)
}

// ── 对话 ─────────────────────────────────────────────────
const roleSelected = ref<roleInfo>()
const infoShow = ref(false)

const startConversation = async (characterId: string | number) => {
  if (!characterId) return
  try {
    const msg = ElMessage.info('正在创建对话…')
    const uuid = await chatHistoryStore().addChatHistory(characterId)
    msg.close()
    router.push(`/chat/${uuid}`)
  } catch {
    ElMessage.error('创建对话失败，请稍后重试')
  }
}

onMounted(async () => {
  const [featuredRes] = await Promise.all([
    roleApi.getChoiceRoleList({ limit: 6 }),
    getRoleList(true),
  ])
  featuredRoles.value = featuredRes.data
})
</script>

<style lang="scss" scoped>
/* ── 版心 ─────────────────────────────────────────────────── */
.discovery {
  display: flex;
  flex-direction: column;
  gap: 32px;
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}

/* ── 页面标题 ─────────────────────────────────────────────── */
.discovery__header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.discovery__title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--vt-text);
}

.discovery__subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--vt-text-muted);
}

/* ── Hero Banner ─────────────────────────────────────────── */
.hero {
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  overflow: hidden;
  background: var(--vt-surface);
  border: 1px solid var(--vt-line);
}

.hero__main {
  display: grid;
  grid-template-columns: 1fr 380px;
  height: 420px;
  position: relative;
}

/* 左侧文案 */
.hero__copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 14px;
  padding: 40px 44px;
  position: relative;
  z-index: 1;
}

.hero__badge {
  align-self: flex-start;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--vt-brand-soft);
  color: var(--vt-brand);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.hero__name {
  margin: 0;
  font-size: 36px;
  font-weight: 800;
  line-height: 1.12;
  letter-spacing: -0.4px;
  color: var(--vt-text);
}

.hero__desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--vt-text-soft);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 34ch;
}

.hero__actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 8px;
}

.hero__cta {
  padding: 12px 28px;
  border: 0;
  border-radius: 999px;
  background: var(--vt-brand);
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;

  &:hover { background: var(--vt-brand-strong); }
  &:active { transform: scale(0.98); }
}

.hero__meta {
  font-size: 13px;
  color: var(--vt-text-muted);
}

/* 右侧图片 — 关键：用 grid 子项撑满高度 */
.hero__visual {
  position: relative;
  overflow: hidden;
  min-height: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: top center;
    display: block;
  }
}

.hero__visual-fade {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to right,
    var(--vt-surface) 0%,
    color-mix(in srgb, var(--vt-surface) 50%, transparent) 25%,
    transparent 60%
  );
  pointer-events: none;
}

/* 图片切换动画 */
.hero-img-enter-active,
.hero-img-leave-active {
  transition: opacity 0.35s ease;
}
.hero-img-enter-from,
.hero-img-leave-to { opacity: 0; }
.hero-img-leave-active { position: absolute; inset: 0; }

/* ── 角色切换区（Banner 内部底部） ────────────────────────── */
.hero__switcher {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--vt-line-subtle);
  background: var(--vt-surface);
}

.hero__switcher-arrow {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border: 1px solid var(--vt-line);
  border-radius: 50%;
  background: var(--vt-surface);
  color: var(--vt-text-soft);
  font-size: 16px;
  cursor: pointer;
  transition: background 0.12s;

  &:hover:not(:disabled) { background: var(--vt-surface-overlay); }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}

.hero__switcher-track {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  flex: 1;

  &::-webkit-scrollbar { display: none; }
}

.hero__switcher-item {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border: 2px solid transparent;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  padding: 0;
  background: var(--vt-surface-overlay);
  transition: border-color 0.15s, box-shadow 0.15s;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: top;
    display: block;
  }

  &:hover { border-color: var(--vt-line); }

  &.is-active {
    border-color: var(--vt-brand);
    box-shadow: 0 0 0 2px var(--vt-brand-soft);
  }
}

/* ── 分类 Tabs ───────────────────────────────────────────── */
.category-tabs {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  scrollbar-width: none;
  padding-top: 8px;

  &::-webkit-scrollbar { display: none; }
}

.category-tabs__item {
  flex-shrink: 0;
  padding: 7px 16px;
  border: 1px solid var(--vt-line);
  border-radius: 999px;
  background: var(--vt-surface);
  color: var(--vt-text-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.12s, color 0.12s, border-color 0.12s;

  &:hover {
    background: var(--vt-surface-overlay);
    color: var(--vt-text);
  }

  &.is-active {
    background: var(--vt-brand);
    border-color: var(--vt-brand);
    color: white;
  }
}

/* ── 内容卡片网格 ────────────────────────────────────────── */
.content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 16px;
}

.content-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;

  &:hover .content-card__media {
    transform: translateY(-3px);
    box-shadow: var(--vt-shadow-md);
  }
}

.content-card__media {
  position: relative;
  aspect-ratio: 2 / 3;
  border-radius: 16px;
  overflow: hidden;
  background: var(--vt-surface-overlay);
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: top;
    display: block;
  }
}

.content-card__gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, oklch(0% 0 0 / 0.72) 0%, oklch(0% 0 0 / 0.15) 45%, transparent 70%);
  pointer-events: none;
}

.content-card__label {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px;

  strong {
    display: block;
    font-size: 14px;
    font-weight: 600;
    color: white;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    display: block;
    font-size: 11px;
    color: oklch(75% 0 0);
    margin-top: 2px;
  }
}

.content-card__desc {
  margin: 0;
  font-size: 12px;
  color: var(--vt-text-muted);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ── 加载更多 ────────────────────────────────────────────── */
.discovery__more {
  display: flex;
  justify-content: center;

  button {
    padding: 10px 32px;
    border: 1px solid var(--vt-line);
    border-radius: 999px;
    background: var(--vt-surface);
    color: var(--vt-text-soft);
    font-size: 14px;
    cursor: pointer;
    transition: background 0.12s;

    &:hover { background: var(--vt-surface-overlay); }
    &:disabled { opacity: 0.5; cursor: not-allowed; }
  }
}

/* ── 响应式 ──────────────────────────────────────────────── */
@media (max-width: 768px) {
  .discovery { padding: 20px 16px 40px; gap: 24px; }

  .hero__main {
    grid-template-columns: 1fr;
    height: auto;
  }

  .hero__visual {
    height: 240px;
    order: -1;
  }

  .hero__visual-fade {
    background: linear-gradient(to top, var(--vt-surface) 0%, transparent 60%);
  }

  .hero__copy { padding: 24px; }
  .hero__name { font-size: 26px; }

  .content-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 10px;
  }
}
</style>
