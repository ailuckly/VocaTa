<template>
  <div class="discovery">
    <!-- ── 页面标题 ──────────────────────────────────────── -->
    <div class="discovery__header">
      <h1 class="discovery__title">✨ 欢迎来到 VocaTa</h1>
    </div>

    <!-- ── Hero Banner（含角色切换） ─────────────────────── -->
    <section class="hero" v-if="heroRole">
      <!-- 模糊背景 -->
      <div class="hero__bg-wrap">
        <Transition name="hero-bg-anim">
          <img :key="heroRole.id" :src="heroRole.avatarUrl" aria-hidden="true" />
        </Transition>
        <div class="hero__bg-overlay"></div>
      </div>

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

          <!-- 角色切换区（左侧内联） -->
          <div class="hero__switcher-inline" v-if="featuredRoles.length > 1">
            <button
              class="hero__switcher-arrow"
              @click="switchHero(-1)"
              aria-label="上一个"
            >‹</button>
            <div class="hero__switcher-track" ref="trackRef">
              <button
                v-for="(role, i) in featuredRoles"
                :key="role.id"
                class="hero__switcher-item"
                :class="{ 'is-active': heroIndex === i }"
                @click="selectHero(i)"
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
              aria-label="下一个"
            >›</button>
          </div>
        </div>

        <!-- 右：角色图卡片 -->
        <div class="hero__visual-card-wrap">
          <div class="hero__visual-card">
            <Transition :name="slideDirection">
              <img
                :key="heroRole.id"
                :src="heroRole.avatarUrl"
                :alt="heroRole.name || ''"
                @error="onAvatarError($event, heroRole.name || '?')"
              />
            </Transition>
            <div class="hero__visual-card-overlay">
              <div class="hero__visual-card-name">{{ heroRole.name }}</div>
              <div class="hero__visual-card-tagline" v-if="heroRole.greeting">
                {{ heroRole.greeting.substring(0, 50) }}{{ heroRole.greeting.length > 50 ? '...' : '' }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ── 角色聊天与分类标题组合 ───────────────────────── -->
    <div class="discovery__section">
      <div class="discovery__section-header">
        <h2 class="discovery__section-title">✨ 角色聊天</h2>
      </div>

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
    </div>

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
            <p class="content-card__desc">{{ role.greeting || role.description || '开始一段对话' }}</p>
          </div>
        </div>
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
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import RoleDialog from './components/RoleDialog.vue'

const router = useRouter()

const TABS = [
  { label: '推荐', value: '' },
  { label: '女', value: '女' },
  { label: '男', value: '男' },
  { label: '🔮 奇幻', value: '奇幻' },
  { label: '🎮 游戏', value: '游戏' },
  { label: '📚 动漫', value: '动漫' },
  { label: '🎬 影视', value: '影视' },
  { label: '🏮 历史', value: '历史' },
  { label: '🛸 科幻', value: '科幻' },
]

// ── 精选 & Hero ──────────────────────────────────────────
const featuredRoles = ref<roleInfo[]>([])
const heroIndex = ref(0)
const heroRole = computed(() => featuredRoles.value[heroIndex.value] ?? null)

// 控制左右滑动动效方向
const slideDirection = ref('slide-left')
const trackRef = ref<HTMLElement | null>(null)

const switchHero = (dir: number) => {
  slideDirection.value = dir > 0 ? 'slide-left' : 'slide-right'
  const len = featuredRoles.value.length
  if (len === 0) return
  // 无极滚动算式
  heroIndex.value = (heroIndex.value + dir + len) % len
}

const selectHero = (i: number) => {
  if (i === heroIndex.value) return
  slideDirection.value = i > heroIndex.value ? 'slide-left' : 'slide-right'
  heroIndex.value = i
}

watch(heroIndex, () => {
  // 当选中项改变时，确保缩略图平滑滚动到视野中央
  nextTick(() => {
    if (trackRef.value && trackRef.value.children[heroIndex.value]) {
      const activeEl = trackRef.value.children[heroIndex.value] as HTMLElement
      activeEl.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' })
    }
  })
})

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
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 32px 32px;
}

/* ── 页面标题 ─────────────────────────────────────────────── */
.discovery__header {
  display: flex;
  flex-direction: column;
}

.discovery__title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: var(--vt-text);
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ── Hero Banner ─────────────────────────────────────────── */
.hero {
  position: relative;
  display: flex;
  flex-direction: column;
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 12px 40px oklch(0% 0 0 / 0.15);
}

.hero__bg-wrap {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

.hero__bg-wrap img {
  position: absolute;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(50px) saturate(1.8); /* 强模糊与提高饱和度 */
  transform: scale(1.15); /* 防止模糊边缘露空白 */
}

.hero__bg-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, oklch(0% 0 0 / 0.75) 0%, oklch(0% 0 0 / 0.45) 100%);
}

.hero-bg-anim-enter-active,
.hero-bg-anim-leave-active {
  transition: opacity 0.8s ease;
}
.hero-bg-anim-enter-from,
.hero-bg-anim-leave-to { opacity: 0; }
.hero-bg-anim-leave-active { position: absolute; inset: 0; }

.hero__main {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap; /* 处理移动端换行 */
  padding: 40px 48px;
}

/* 左侧文案 */
.hero__copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
  padding: 10px 0;
  flex: 1;
  min-width: 320px;
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
  font-size: 38px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.4px;
  color: #ffffff; /* 深色背景上采用白字 */
}

.hero__desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: oklch(90% 0 0); /* 浅灰偏白 */
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 460px;
}

.hero__actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 4px;
  margin-bottom: 16px;
}

.hero__cta {
  padding: 12px 32px;
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
  color: oklch(80% 0 0);
}

/* 角色切换区（内联） */
.hero__switcher-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.hero__switcher-arrow {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border: 1px solid oklch(100% 0 0 / 0.3);
  border-radius: 50%;
  background: transparent;
  color: #ffffff;
  font-size: 18px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;

  &:hover:not(:disabled) { 
    background: oklch(100% 0 0 / 0.1);
    border-color: oklch(100% 0 0 / 0.5);
  }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}

.hero__switcher-track {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scrollbar-width: none;
  scroll-behavior: smooth;
  max-width: 100%;
  
  &::-webkit-scrollbar { display: none; }
}

.hero__switcher-item {
  width: 58px;
  height: 80px;
  flex-shrink: 0;
  border: 2px solid transparent;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  padding: 0;
  background: var(--vt-surface-overlay);
  transition: transform 0.15s, border-color 0.15s;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: top;
    display: block;
  }

  &:hover { transform: translateY(-2px); }

  &.is-active {
    border-color: var(--vt-brand);
  }
}

/* 右侧图片卡片 */
.hero__visual-card-wrap {
  flex-shrink: 0;
  width: 360px; /* 适中大小 (原 480px，上一版 288px) */
  display: flex;
  justify-content: flex-end;
}

.hero__visual-card {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 24px;
  overflow: hidden;
  background: var(--vt-surface-overlay);
  box-shadow: 0 12px 40px oklch(0% 0 0 / 0.15);
  border: 1px solid var(--vt-line-subtle);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: top;
    display: block;
  }
}

/* 动效切换：卡片滑动动画 */
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.55s cubic-bezier(0.25, 1, 0.5, 1);
}

.slide-left-enter-from { opacity: 0; transform: translateX(30px); }
.slide-left-leave-to { opacity: 0; transform: translateX(-30px) scale(0.98); }

.slide-right-enter-from { opacity: 0; transform: translateX(-30px); }
.slide-right-leave-to { opacity: 0; transform: translateX(30px) scale(0.98); }

.slide-left-leave-active,
.slide-right-leave-active { position: absolute; inset: 0; }

.hero__visual-card-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 30px 24px 24px;
  background: linear-gradient(to top, oklch(0% 0 0 / 0.8) 0%, transparent 100%);
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
  text-align: center;
}

.hero__visual-card-name {
  font-size: 20px;
  font-weight: 700;
  color: white;
  border-bottom: 2px solid var(--vt-brand);
  padding-bottom: 4px;
  display: inline-block;
}

.hero__visual-card-tagline {
  font-size: 13px;
  color: oklch(90% 0 0);
  margin-top: 4px;
}

/* ── 角色聊天区块标题 ────────────────────────────────────── */
.discovery__section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.discovery__section-header {
  display: flex;
  align-items: center;
}

.discovery__section-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--vt-text);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ── 分类 Tabs ───────────────────────────────────────────── */
.category-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  padding-bottom: 8px;

  &::-webkit-scrollbar { display: none; }
}

.category-tabs__item {
  flex-shrink: 0;
  padding: 8px 18px;
  border: 0;
  border-radius: 999px;
  background: var(--vt-surface);
  color: var(--vt-text-soft);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;

  &:hover {
    background: var(--vt-surface-overlay);
    color: var(--vt-text);
  }

  &.is-active {
    background: var(--vt-brand);
    color: white;
  }
}

/* ── 内容卡片网格 ────────────────────────────────────────── */
.content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.content-card {
  display: flex;
  flex-direction: column;
  cursor: pointer;
  height: 100%;

  &:hover .content-card__media {
    transform: translateY(-4px);
    box-shadow: 0 10px 24px oklch(0% 0 0 / 0.12);
  }
}

.content-card__media {
  position: relative;
  aspect-ratio: 9 / 13;
  border-radius: 16px;
  overflow: hidden;
  background: var(--vt-surface-overlay);
  transition: transform 0.25s ease, box-shadow 0.25s ease;

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
  background: linear-gradient(to top, oklch(0% 0 0 / 0.85) 0%, oklch(0% 0 0 / 0.4) 40%, transparent 60%);
  pointer-events: none;
}

.content-card__label {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;

  strong {
    display: block;
    font-size: 15px;
    font-weight: 600;
    color: white;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.content-card__desc {
  margin: 0;
  font-size: 12px;
  color: oklch(80% 0 0);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ── 加载更多 ────────────────────────────────────────────── */
.discovery__more {
  display: flex;
  justify-content: center;
  margin-top: 10px;

  button {
    padding: 12px 36px;
    border: 1px solid var(--vt-line);
    border-radius: 999px;
    background: transparent;
    color: var(--vt-text-soft);
    font-size: 14px;
    cursor: pointer;
    transition: background 0.12s;

    &:hover { background: var(--vt-surface-overlay); }
    &:disabled { opacity: 0.5; cursor: not-allowed; }
  }
}

/* ── 响应式 ──────────────────────────────────────────────── */
@media (max-width: 900px) {
  .hero__main { flex-direction: column-reverse; justify-content: flex-end; padding: 32px 24px; }
  .hero__visual-card-wrap { width: 100%; max-width: 360px; align-self: center; }
  .hero__copy { align-items: center; text-align: center; }
  .hero__badge { align-self: center; }
  .hero__switcher-inline { justify-content: center; }
  .hero__name { font-size: 32px; }
}

@media (max-width: 768px) {
  .discovery { padding: 20px 16px 40px; gap: 28px; }
  .content-grid { gap: 12px; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); }
}
</style>
