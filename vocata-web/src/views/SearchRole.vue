<template>
  <div class="home-shell">
    <div class="home-container">
      <header class="home-topbar">
        <div class="home-topbar__left"></div>
        <label class="search-box">
          <span class="search-icon">⌕</span>
          <input v-model.trim="searchKeyword" type="search" placeholder="搜索角色、设定、剧情或你想聊的内容" />
        </label>
        <div class="home-topbar__actions">
          <RouterLink to="/newRole" class="home-topbar__create">
            <el-icon><Plus /></el-icon> 创建角色
          </RouterLink>
          <button type="button" class="home-topbar__member" @click="goMember">
            会员
          </button>
          <button type="button" class="home-topbar__icon" aria-label="通知">
            <el-icon><Bell /></el-icon>
          </button>
          <button type="button" class="home-topbar__avatar" aria-label="用户中心">
            <img src="/avatars/alice.png" alt="" />
          </button>
        </div>
      </header>

      <div class="home-content-grid">
        <main class="main-column">

        <HomeHero
          v-if="heroRole"
          badge="今日推荐"
          :role="heroRole"
          :tags="heroTags"
          :description="heroDescription"
          :switch-roles="heroSwitchRoles"
          :active-index="heroIndex"
          @chat="startConversation"
          @detail="openRoleDetail"
          @select="selectHero"
        />

        <CategoryChips :categories="CATEGORY_CHIPS" :active-category="activeCategory" @select="selectCategory" />

        <ContinueChatList v-if="continueChats.length > 0" :items="continueChats" @continue="resumeConversation" />

        <FeaturedSection
          title="编辑精选"
          variant="visual"
          :featured-items="featuredCardItems"
          @chat="startConversation"
          @detail="openStaticRoleDetail"
        />

        <FeaturedSection
          title="热门角色"
          variant="role"
          :role-items="hotRoleItems"
          @chat="startConversation"
          @detail="openStaticRoleDetail"
        />

        <FeaturedSection
          title="猜你喜欢"
          variant="soft"
          :role-items="recommendedRoleItems"
          @chat="startConversation"
          @detail="openStaticRoleDetail"
        />
        </main>

        <aside class="right-rail">
          <RightDiscoveryPanel
            :tags="HOT_TAGS"
            :topics="TOPICS"
            :creators="CREATORS"
            :followed-names="followedCreators"
            @tag="selectCategory"
            @follow="toggleFollowCreator"
            @member="goMember"
          />
        </aside>
      </div>
    </div>

    <RoleDialog :item="roleSelected" v-if="infoShow && roleSelected" @close="infoShow = false" />
  </div>
</template>

<script setup lang="ts">
import { roleApi } from '@/api/modules/role'
import CategoryChips from '@/components/home/CategoryChips.vue'
import ContinueChatList from '@/components/home/ContinueChatList.vue'
import FeaturedSection from '@/components/home/FeaturedSection.vue'
import HomeHero from '@/components/home/HomeHero.vue'
import RightDiscoveryPanel from '@/components/home/RightDiscoveryPanel.vue'
import {
  CATEGORY_CHIPS,
  CREATORS,
  HOT_TAGS,
  TOPICS,
  roleToCard
} from '@/components/home/homeData'
import type { FeaturedCardData } from '@/components/home/homeData'
import { chatHistoryStore } from '@/store'
import type { roleInfo } from '@/types/common'
import { ElMessage } from 'element-plus'
import { Plus, Bell } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import RoleDialog from './components/RoleDialog.vue'

const router = useRouter()

const featuredRoles = ref<roleInfo[]>([])
const roleList = ref<roleInfo[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 16
const loading = ref(false)
const activeCategory = ref('推荐')
const searchKeyword = ref('')
const heroIndex = ref(0)
const roleSelected = ref<roleInfo>()
const infoShow = ref(false)
const followedCreators = ref<string[]>([])

const heroFallback: roleInfo = {
  id: 9,
  name: '哈利·波特',
  greeting: '霍格沃茨的夜晚刚刚开始。想和我一起探索禁林、学习咒语，还是聊聊你的烦恼？',
  description: '魔法校园冒险',
  avatarUrl: '/avatars/harry.png',
  tags: ['魔法', '冒险', '勇气', '校园'],
  chatCount: 289000,
}

const heroSwitchRoles = computed(() => {
  const roles = featuredRoles.value.length ? featuredRoles.value : [heroFallback]
  return roles.slice(0, 5)
})
const heroRole = computed(() => heroSwitchRoles.value[heroIndex.value] ?? heroFallback)
const heroTags = computed(() => getRoleTags(heroRole.value).slice(0, 4))
const heroDescription = computed(() =>
  heroRole.value.greeting || heroRole.value.description || heroFallback.greeting || '从一句开场白开始进入角色世界。'
)
const publicRoleCards = computed(() => roleList.value.map(roleToCard))

const roleToFeaturedCard = (role: roleInfo): FeaturedCardData => ({
  id: role.id,
  name: role.name || '未命名角色',
  tags: [role.description || role.greeting || '精彩设定不容错过'],
  count: String(role.chatCount || 0),
  cover: role.avatarUrl || '/avatars/storydice.png'
})

const featuredCardItems = computed(() => featuredRoles.value.map(roleToFeaturedCard))

const hotRoleItems = computed(() => {
  const keyword = searchKeyword.value.toLowerCase()
  if (!keyword) return publicRoleCards.value.slice(0, 4)
  return publicRoleCards.value.filter((role) => `${role.name} ${role.desc}`.toLowerCase().includes(keyword)).slice(0, 4)
})

const recommendedRoleItems = computed(() => {
  return publicRoleCards.value.slice(4, 12)
})

const historyStore = chatHistoryStore()
const continueChats = computed(() => {
  return historyStore.chatHistory.slice(0, 8).map(item => ({
    id: item.conversationUuid,
    name: item.characterName || item.title || '未命名对话',
    lastTopic: item.lastMessageSummary || item.greeting || '继续聊天',
    avatar: item.characterAvatarUrl || '/avatars/dialogue.png'
  }))
})



const getRoleTags = (role: roleInfo) => {
  if (Array.isArray(role.tags)) {
    return role.tags.filter(Boolean)
  }
  if (typeof role.tags === 'string' && role.tags.trim()) {
    return role.tags.split(/[,，\s]+/).filter(Boolean)
  }
  return ['魔法', '冒险', '勇气', '校园']
}

const getRoleList = async (reset = false) => {
  if (loading.value) return
  loading.value = true
  try {
    const categoryTag = activeCategory.value === '推荐' || activeCategory.value === '更多' ? '' : activeCategory.value
    const res = await roleApi.getPublicRoleList({
      pageNum: pageNum.value,
      pageSize,
      orderDirection: 'desc',
      tags: categoryTag ? [categoryTag] : undefined,
    })
    roleList.value = reset ? res.data.list : [...roleList.value, ...res.data.list]
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const selectCategory = async (category: string) => {
  activeCategory.value = category
  pageNum.value = 1
  await getRoleList(true)
}

const selectHero = (index: number) => {
  heroIndex.value = index
}

const openRoleDetail = (role: roleInfo) => {
  roleSelected.value = role
  infoShow.value = true
}

const openStaticRoleDetail = (id: number) => {
  const role = roleList.value.find((item) => item.id === id) || featuredRoles.value.find((item) => item.id === id)
  if (role) {
    openRoleDetail(role)
  } else {
    // No static detail route exists yet; keep a non-breaking placeholder interaction.
    ElMessage.info('角色详情即将开放')
  }
}

const startConversation = async (characterId: string | number) => {
  if (!characterId) return
  try {
    const msg = ElMessage.info('正在创建对话...')
    const uuid = await historyStore.addChatHistory(characterId)
    msg.close()
    router.push(`/chat/${uuid}`)
  } catch {
    ElMessage.error('创建对话失败，请稍后重试')
  }
}

const resumeConversation = (uuid: string | number) => {
  router.push(`/chat/${uuid}`)
}

const toggleFollowCreator = (name: string) => {
  followedCreators.value = followedCreators.value.includes(name)
    ? followedCreators.value.filter((item) => item !== name)
    : [...followedCreators.value, name]
}

const goMember = () => {
  ElMessage.info('会员中心即将开放')
}

onMounted(async () => {
  try {
    const [featuredRes] = await Promise.all([
      roleApi.getChoiceRoleList({ limit: 6 }).catch(() => ({ data: [] })),
      getRoleList(true).catch(() => {}),
      historyStore.getChatHistory().catch(() => {})
    ])
    featuredRoles.value = featuredRes.data?.length ? (featuredRes.data as roleInfo[]) : [heroFallback]
  } catch (error) {
    console.error('Failed to load homepage data:', error)
    featuredRoles.value = [heroFallback]
  }
})
</script>

<style lang="scss">
.home-section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  h2 {
    margin: 0;
    color: #171b33;
    font-size: 18px;
    line-height: 1.2;
    font-weight: bold;
  }

  button {
    border: 0;
    color: #a0a5b5;
    background: transparent;
    cursor: pointer;
    font-size: 13px;
    font-weight: normal;
    
    &:hover {
      color: #7755ff;
    }
  }
}
</style>

<style scoped lang="scss">
.home-shell {
  min-width: 0;
  background: #f5f7ff;
  color: #18213d;
  min-height: 100vh;
}

.home-container {
  max-width: 1400px;
  margin: 0 auto;
}

.home-content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 20px;
  align-items: start;
}

.main-column {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.right-rail {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.home-topbar {
  position: relative;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.home-topbar__left {
  flex: 1;
}

.search-box {
  position: absolute;
  /* Center exactly over the main-column (which excludes the 280px right-rail and 20px gap) */
  left: calc(50% - 150px);
  transform: translateX(-50%);
  width: 500px;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  border-radius: 999px;
  border: 1px solid rgba(129, 140, 248, 0.22);
  background: rgba(255, 255, 255, 0.82);
  padding: 0 16px;
  transition: all 0.2s;
  
  &:focus-within {
    border-color: #7c5cff;
    background: #ffffff;
    box-shadow: 0 4px 12px rgba(124, 92, 255, 0.08);
  }

  .search-icon {
    color: #a0a5b5;
    font-size: 16px;
    font-weight: bold;
  }

  input {
    width: 100%;
    border: 0;
    outline: 0;
    color: #171b33;
    background: transparent;
    font-size: 13px;

    &::placeholder {
      color: #a0a5b5;
    }
  }
}

.home-topbar__actions {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.home-topbar__create,
.home-topbar__member,
.home-topbar__icon,
.home-topbar__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  text-decoration: none;
  font-size: 13px;
}

.home-topbar__create {
  border: 0;
  padding: 0 14px;
  height: 34px;
  color: white;
  background: #7755ff;
  border-radius: 8px;
  font-weight: 500;
  gap: 6px;
  transition: all 0.2s;

  .el-icon {
    font-size: 14px;
  }

  &:hover {
    background: #6547ea;
  }
}

.home-topbar__member {
  border: 1px solid rgba(129, 140, 248, 0.22);
  padding: 0 14px;
  height: 34px;
  color: #68708f;
  background: rgba(255, 255, 255, 0.78);
  border-radius: 8px;
  font-weight: 600;
  gap: 6px;
  transition: all 0.2s;

  &:hover {
    background: #ffffff;
  }
}

.home-topbar__icon,
.home-topbar__avatar {
  width: 34px;
  height: 34px;
  border: 1px solid #ebedf5;
  border-radius: 50%;
  color: #5c6275;
  background: white;
  transition: all 0.2s;
}

.home-topbar__icon {
  &:hover {
    background: #f7f8fa;
    color: #171b33;
  }
}

.home-topbar__avatar {
  overflow: hidden;
  padding: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

@media (max-width: 1280px) {
  .home-content-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  
  .right-rail {
    display: none;
  }
  
  .search-box {
    /* Restore perfect center when right-rail is hidden */
    left: 50%;
  }
}

@media (max-width: 960px) {
  .home-shell {
    padding: 16px;
  }
  
  .home-topbar {
    flex-direction: column;
    gap: 16px;
    height: auto;
  }
  
  .search-box {
    position: relative;
    left: 0;
    transform: none;
    width: 100%;
  }
}

@media (max-width: 640px) {
  .home-topbar__member,
  .home-topbar__icon {
    display: none;
  }
}
</style>
