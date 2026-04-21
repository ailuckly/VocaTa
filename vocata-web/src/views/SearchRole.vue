<template>
  <div class="discovery-page">
    <!-- Hero + 精选网格 -->
    <section class="discovery-page__hero-section">
      <!-- 左侧 Hero 单角色 -->
      <div class="discovery-hero" v-if="heroRole" @click="startConversation(heroRole.id)">
        <img
          class="discovery-hero__bg"
          :src="heroRole.avatarUrl"
          :alt="heroRole.name || ''"
          @error="onAvatarError($event, heroRole.name || '?')"
        />
        <div class="discovery-hero__overlay"></div>
        <div class="discovery-hero__content">
          <p class="discovery-hero__eyebrow">精选角色</p>
          <h1 class="discovery-hero__name">{{ heroRole.name }}</h1>
          <p class="discovery-hero__desc">{{ heroRole.greeting || heroRole.description }}</p>
          <button class="discovery-hero__cta" @click.stop="startConversation(heroRole.id)">
            开始对话
          </button>
        </div>
      </div>

      <!-- 右侧精选卡片网格 -->
      <div class="discovery-featured-grid">
        <div
          v-for="role in featuredGrid"
          :key="role.id"
          class="discovery-featured-card"
          @click="startConversation(role.id)"
        >
          <img
            :src="role.avatarUrl"
            :alt="role.name || ''"
            @error="onAvatarError($event, role.name || '?')"
          />
          <div class="discovery-featured-card__overlay"></div>
          <div class="discovery-featured-card__info">
            <strong>{{ role.name }}</strong>
            <span>{{ role.chatCount ? role.chatCount + ' 次对话' : '' }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Tag 过滤栏 -->
    <div class="discovery-tag-bar">
      <button
        v-for="tag in tagList"
        :key="tag.value"
        class="discovery-tag-bar__pill"
        :class="{ 'is-active': activeTag === tag.value }"
        @click="selectTag(tag.value)"
      >
        <span v-if="tag.emoji" class="discovery-tag-bar__emoji">{{ tag.emoji }}</span>
        {{ tag.label }}
      </button>
    </div>

    <!-- 角色网格 -->
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
            <span v-if="role.chatCount">{{ role.chatCount }} 次对话</span>
          </div>
        </div>
        <p class="discovery-card__desc">{{ role.greeting || role.description || '开始一段对话' }}</p>
      </div>
    </section>

    <!-- 加载更多 -->
    <div v-if="hasMore" class="discovery-page__more">
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
import { isMobile } from '@/utils/isMobile'
import { onAvatarError } from '@/utils/avatar'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import RoleDialog from './components/RoleDialog.vue'

const router = useRouter()
const isM = computed(() => isMobile())

// 精选角色
const featuredRoles = ref<roleInfo[]>([])
const heroRole = computed(() => featuredRoles.value[0] ?? null)
const featuredGrid = computed(() => featuredRoles.value.slice(1, 6))

// 角色列表
const roleList = ref<roleInfo[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 15
const loading = ref(false)
const hasMore = computed(() => roleList.value.length < total.value)

// Tag 过滤
const TAG_OPTIONS = [
  { label: '全部', value: '', emoji: '' },
  { label: '女', value: '女', emoji: '👩' },
  { label: '男', value: '男', emoji: '👨' },
  { label: '影视', value: '影视', emoji: '🎬' },
  { label: '奇幻', value: '奇幻', emoji: '✨' },
  { label: '游戏', value: '游戏', emoji: '🎮' },
  { label: '动漫', value: '动漫', emoji: '🌸' },
  { label: '历史', value: '历史', emoji: '📜' },
  { label: '科幻', value: '科幻', emoji: '🚀' },
  { label: '现代', value: '现代', emoji: '🏙️' },
]
const tagList = ref(TAG_OPTIONS)
const activeTag = ref('')

// 对话框
const roleSelected = ref<roleInfo>()
const infoShow = ref(false)

const getRoleList = async (reset = false) => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await roleApi.getPublicRoleList({
      pageNum: pageNum.value,
      pageSize,
      orderDirection: 'desc',
      tags: activeTag.value ? [activeTag.value] : undefined,
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

const selectTag = async (tag: string) => {
  if (activeTag.value === tag) return
  activeTag.value = tag
  pageNum.value = 1
  await getRoleList(true)
}

const loadMore = async () => {
  pageNum.value++
  await getRoleList(false)
}

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
.discovery-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
}

/* ── Hero Section ─────────────────────────────────────────── */
.discovery-page__hero-section {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 12px;
  height: 420px;
}

/* Hero 左侧大图 */
.discovery-hero {
  position: relative;
  border-radius: var(--vt-radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;

  &:hover { transform: scale(1.01); }
}

.discovery-hero__bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.discovery-hero__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to top,
    oklch(0% 0 0 / 0.85) 0%,
    oklch(0% 0 0 / 0.3) 50%,
    transparent 100%
  );
}

.discovery-hero__content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 20px;
  gap: 6px;
}

.discovery-hero__eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: oklch(80% 0.12 270);
}

.discovery-hero__name {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: white;
  line-height: 1.2;
}

.discovery-hero__desc {
  margin: 0;
  font-size: 13px;
  color: oklch(85% 0 0);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.discovery-hero__cta {
  align-self: flex-start;
  margin-top: 4px;
  padding: 8px 16px;
  border: 0;
  border-radius: 999px;
  background: white;
  color: oklch(15% 0 0);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;

  &:hover { background: oklch(90% 0 0); }
}

/* 右侧精选网格 */
.discovery-featured-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 8px;
}

.discovery-featured-card {
  position: relative;
  border-radius: var(--vt-radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.18s;

  &:hover { transform: scale(1.03); }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.discovery-featured-card__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, oklch(0% 0 0 / 0.7) 0%, transparent 55%);
}

.discovery-featured-card__info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 1px;

  strong {
    font-size: 12px;
    font-weight: 600;
    color: white;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    font-size: 10px;
    color: oklch(75% 0 0);
  }
}

/* ── Tag Bar ──────────────────────────────────────────────── */
.discovery-tag-bar {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;

  &::-webkit-scrollbar { display: none; }
}

.discovery-tag-bar__pill {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border: 1px solid var(--vt-line);
  border-radius: 999px;
  background: var(--vt-surface);
  color: var(--vt-text-soft);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.12s, color 0.12s, border-color 0.12s;
  flex-shrink: 0;

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

.discovery-tag-bar__emoji {
  font-size: 14px;
}

/* ── Role Grid ────────────────────────────────────────────── */
.discovery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.discovery-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;

  &:hover .discovery-card__media {
    transform: translateY(-2px);
    box-shadow: var(--vt-shadow-md);
  }
}

.discovery-card__media {
  position: relative;
  aspect-ratio: 2 / 3;
  border-radius: var(--vt-radius-md);
  overflow: hidden;
  transition: transform 0.18s, box-shadow 0.18s;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.discovery-card__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, oklch(0% 0 0 / 0.65) 0%, transparent 50%);
}

.discovery-card__info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px;

  strong {
    display: block;
    font-size: 13px;
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

/* ── Load More ────────────────────────────────────────────── */
.discovery-page__more {
  display: flex;
  justify-content: center;
  padding: 8px 0 16px;

  button {
    padding: 10px 28px;
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

/* ── Responsive ───────────────────────────────────────────── */
@media (max-width: 1024px) {
  .discovery-page__hero-section {
    grid-template-columns: 220px 1fr;
    height: 340px;
  }

  .discovery-featured-grid {
    grid-template-columns: repeat(4, 1fr);
    grid-template-rows: repeat(2, 1fr);

    // 第5张隐藏
    > :nth-child(5) { display: none; }
  }
}

@media (max-width: 768px) {
  .discovery-page {
    padding: 16px;
    gap: 16px;
  }

  .discovery-page__hero-section {
    grid-template-columns: 1fr;
    height: auto;
  }

  .discovery-hero {
    height: 280px;
  }

  .discovery-featured-grid {
    grid-template-columns: repeat(4, 1fr);
    grid-template-rows: 1fr;
    height: 120px;

    > :nth-child(n+5) { display: none; }
  }

  .discovery-grid {
    grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
    gap: 8px;
  }
}
</style>
