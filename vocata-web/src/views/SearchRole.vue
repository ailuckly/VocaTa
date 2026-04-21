<template>
  <div class="discovery-page">

    <!-- ── Hero Banner ─────────────────────────────────────── -->
    <section class="discovery-hero" v-if="heroRole">
      <!-- 右侧大图 -->
      <div class="discovery-hero__image">
        <Transition name="hero-fade">
          <img
            :key="heroRole.id"
            :src="heroRole.avatarUrl"
            :alt="heroRole.name || ''"
            @error="onAvatarError($event, heroRole.name || '?')"
          />
        </Transition>
        <div class="discovery-hero__image-overlay"></div>
      </div>
      <!-- 左侧文案 -->
      <div class="discovery-hero__copy">
        <p class="discovery-hero__eyebrow">精选角色</p>
        <h1 class="discovery-hero__title">{{ heroRole.name }}</h1>
        <p class="discovery-hero__desc">{{ heroRole.greeting || heroRole.description || '开始一段真实的陪伴对话' }}</p>
        <div class="discovery-hero__actions">
          <button class="discovery-hero__cta" @click="startConversation(heroRole.id)">
            立即体验
          </button>
          <span class="discovery-hero__meta" v-if="heroRole.chatCount">
            {{ heroRole.chatCount.toLocaleString() }} 次对话
          </span>
        </div>
      </div>
    </section>

    <!-- ── Carousel 精选横向列表 ───────────────────────────── -->
    <section class="discovery-carousel" v-if="featuredRoles.length">
      <div class="discovery-carousel__track" ref="carouselRef">
        <button
          v-for="role in featuredRoles"
          :key="role.id"
          class="discovery-carousel__item"
          :class="{ 'is-active': heroRole?.id === role.id }"
          @click="selectHero(role)"
        >
          <div class="discovery-carousel__thumb">
            <img
              :src="role.avatarUrl"
              :alt="role.name || ''"
              @error="onAvatarError($event, role.name || '?')"
            />
          </div>
          <span>{{ role.name }}</span>
        </button>
      </div>
    </section>

    <!-- ── Tab 分类栏 ──────────────────────────────────────── -->
    <nav class="discovery-tabs">
      <button
        v-for="tab in TABS"
        :key="tab.value"
        class="discovery-tabs__item"
        :class="{ 'is-active': activeTab === tab.value }"
        @click="selectTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </nav>

    <!-- ── 角色卡片网格 ────────────────────────────────────── -->
    <section class="discovery-grid">
      <div
        v-for="role in roleList"
        :key="role.id"
        class="discovery-card"
        @click="startConversation(role.id)"
      >
        <div class="discovery-card__media">
          <img
            :src="role.avatarUrl"
            :alt="role.name || ''"
            @error="onAvatarError($event, role.name || '?')"
          />
          <div class="discovery-card__overlay"></div>
          <div class="discovery-card__info">
            <strong>{{ role.name }}</strong>
            <span v-if="role.chatCount">{{ role.chatCount.toLocaleString() }} 次对话</span>
          </div>
        </div>
        <p class="discovery-card__desc">{{ role.greeting || role.description || '开始一段对话' }}</p>
      </div>
    </section>

    <!-- 加载更多 -->
    <div v-if="hasMore" class="discovery-more">
      <button @click="loadMore" :disabled="loading">
        {{ loading ? '加载中…' : '加载更多' }}
      </button>
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
import { computed, onMounted, ref } from 'vue'
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

// 精选 & Hero
const featuredRoles = ref<roleInfo[]>([])
const heroRole = ref<roleInfo | null>(null)
const carouselRef = ref<HTMLElement | null>(null)

const selectHero = (role: roleInfo) => {
  heroRole.value = role
}

// 角色列表
const roleList = ref<roleInfo[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 15
const loading = ref(false)
const hasMore = computed(() => roleList.value.length < total.value)

// Tab
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
    if (reset) {
      roleList.value = res.data.list
    } else {
      roleList.value = [...roleList.value, ...res.data.list]
    }
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

// 对话框
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
  if (featuredRes.data.length) heroRole.value = featuredRes.data[0]
})
</script>

<style lang="scss" scoped>
/* ── 页面容器 ─────────────────────────────────────────────── */
.discovery-page {
  display: flex;
  flex-direction: column;
  gap: 28px;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 40px;
}

/* ── Hero Banner ─────────────────────────────────────────── */
.discovery-hero {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 420px;
  align-items: center;
  gap: 0;
  min-height: 420px;
  border-radius: 20px;
  overflow: hidden;
  background: var(--vt-surface);
  border: 1px solid var(--vt-line);
}

/* 右侧图片区 */
.discovery-hero__image {
  position: absolute;
  inset: 0;
  left: auto;
  width: 420px;
  right: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: top center;
    display: block;
  }
}

.discovery-hero__image-overlay {
  position: absolute;
  inset: 0;
  /* 左侧渐变遮罩，让文字区域清晰 */
  background: linear-gradient(
    to right,
    var(--vt-surface) 0%,
    color-mix(in srgb, var(--vt-surface) 80%, transparent) 50%,
    transparent 100%
  );
}

/* 左侧文案 */
.discovery-hero__copy {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 48px 48px 48px 48px;
  max-width: 480px;
}

.discovery-hero__eyebrow {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--vt-brand);
}

.discovery-hero__title {
  margin: 0;
  font-size: clamp(28px, 3vw, 42px);
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.5px;
  color: var(--vt-text);
}

.discovery-hero__desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--vt-text-soft);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 36ch;
}

.discovery-hero__actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 4px;
}

.discovery-hero__cta {
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

.discovery-hero__meta {
  font-size: 13px;
  color: var(--vt-text-muted);
}

/* Hero 图片切换动画 */
.hero-fade-enter-active,
.hero-fade-leave-active {
  transition: opacity 0.4s ease;
  position: absolute;
  inset: 0;
}
.hero-fade-enter-from,
.hero-fade-leave-to { opacity: 0; }

/* ── Carousel ────────────────────────────────────────────── */
.discovery-carousel {
  overflow: hidden;
}

.discovery-carousel__track {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;

  &::-webkit-scrollbar { display: none; }
}

.discovery-carousel__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  padding: 4px;
  border-radius: var(--vt-radius-md);
  transition: opacity 0.15s;

  &:hover { opacity: 0.85; }

  span {
    font-size: 12px;
    font-weight: 500;
    color: var(--vt-text-soft);
    white-space: nowrap;
    max-width: 72px;
    overflow: hidden;
    text-overflow: ellipsis;
    transition: color 0.15s;
  }

  &.is-active {
    span { color: var(--vt-brand); font-weight: 600; }

    .discovery-carousel__thumb {
      border-color: var(--vt-brand);
      box-shadow: 0 0 0 3px var(--vt-brand-soft);
    }
  }
}

.discovery-carousel__thumb {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  overflow: hidden;
  border: 2px solid var(--vt-line);
  transition: border-color 0.15s, box-shadow 0.15s;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: top;
    display: block;
  }
}

/* ── Tabs ────────────────────────────────────────────────── */
.discovery-tabs {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
  padding-bottom: 2px;

  &::-webkit-scrollbar { display: none; }
}

.discovery-tabs__item {
  flex-shrink: 0;
  padding: 8px 18px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--vt-text-soft);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.12s, color 0.12s;

  &:hover {
    background: var(--vt-surface-overlay);
    color: var(--vt-text);
  }

  &.is-active {
    background: var(--vt-brand);
    color: white;
  }
}

/* ── Card Grid ───────────────────────────────────────────── */
.discovery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 16px;
}

.discovery-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;

  &:hover .discovery-card__media {
    transform: translateY(-3px);
    box-shadow: var(--vt-shadow-md);
  }
}

.discovery-card__media {
  position: relative;
  aspect-ratio: 2 / 3;
  border-radius: 16px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  background: var(--vt-surface-overlay);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: top;
    display: block;
  }
}

.discovery-card__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to top,
    oklch(0% 0 0 / 0.75) 0%,
    oklch(0% 0 0 / 0.2) 45%,
    transparent 70%
  );
}

.discovery-card__info {
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

.discovery-card__desc {
  margin: 0;
  font-size: 12px;
  color: var(--vt-text-muted);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ── Load More ───────────────────────────────────────────── */
.discovery-more {
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
    transition: background 0.12s, color 0.12s;

    &:hover { background: var(--vt-surface-overlay); color: var(--vt-text); }
    &:disabled { opacity: 0.5; cursor: not-allowed; }
  }
}

/* ── Dark mode enhancements ──────────────────────────────── */
[data-theme="dark"] {
  .discovery-page { background: #0b1020; }

  .discovery-hero {
    background: #111827;
    border-color: oklch(28% 0.01 240);
  }

  .discovery-hero__image-overlay {
    background: linear-gradient(
      to right,
      #111827 0%,
      color-mix(in srgb, #111827 75%, transparent) 50%,
      transparent 100%
    );
  }
}

/* ── Responsive ──────────────────────────────────────────── */
@media (max-width: 900px) {
  .discovery-hero {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .discovery-hero__image {
    position: relative;
    width: 100%;
    height: 260px;
    left: 0;
    right: 0;
    order: -1;
  }

  .discovery-hero__image-overlay {
    background: linear-gradient(
      to top,
      var(--vt-surface) 0%,
      transparent 60%
    );
  }

  .discovery-hero__copy {
    padding: 24px;
    max-width: 100%;
  }
}

@media (max-width: 640px) {
  .discovery-page { padding: 16px 16px 32px; gap: 20px; }

  .discovery-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 10px;
  }
}
</style>
