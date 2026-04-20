<template>
  <section :class="isM ? 'mobile' : 'pc'" class="discovery-page">
    <DiscoveryHero :selected-roles="selectRoleList" @start="startConversation" />

    <RoleFilterBar
      v-model:keyword="searchInput"
      :current-view="currentView"
      :order-direction="searchParam.orderDirection"
      @change-view="setViewMode"
      @search="search"
    />

    <RoleShelf :roles="roleList" @detail="openRoleDialog" @start="startConversation" />

    <div class="pagination-container">
      <el-pagination
        :size="isM ? 'small' : 'default'"
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="searchParam.pageSize"
        :current-page="searchParam.pageNum"
        @current-change="handlePageChange"
        hide-on-single-page
      />
    </div>

    <RoleDialog :item="roleSelected" v-if="infoShow && roleSelected" @close="infoShow = false" />
  </section>
</template>

<script setup lang="ts">
import { roleApi } from '@/api/modules/role'
import { chatHistoryStore } from '@/store'
import type { PublicRoleQuery } from '@/types/api'
import type { roleInfo } from '@/types/common'
import debounce from '@/types/debounce'
import { isMobile } from '@/utils/isMobile'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'
import type { Ref } from 'vue'
import { useRouter } from 'vue-router'
import DiscoveryHero from '@/components/discovery/DiscoveryHero.vue'
import RoleFilterBar from '@/components/discovery/RoleFilterBar.vue'
import RoleShelf from '@/components/discovery/RoleShelf.vue'
import RoleDialog from './components/RoleDialog.vue'

const isM = computed(() => isMobile())
const router = useRouter()

const roleSelected = ref<roleInfo>()
const searchInput = ref('')
const roleList: Ref<roleInfo[]> = ref([])
const selectRoleList: Ref<roleInfo[]> = ref([])
const searchParam: Ref<PublicRoleQuery> = ref({
  pageNum: 1,
  pageSize: 15,
  orderDirection: 'desc',
})
const total = ref(0)
const currentView = ref('public')
const infoShow = ref(false)

watch(
  [searchParam, currentView],
  () => {
    if (currentView.value === 'public') {
      getRoleList()
    } else if (currentView.value === 'my') {
      getMyRoleList()
    }
  },
  { deep: true },
)

const getRoleList = async () => {
  const res = await roleApi.getPublicRoleList(searchParam.value)
  roleList.value = res.data.list
  total.value = res.data.total
}

const getSelectedRoleList = async () => {
  const res = await roleApi.getChoiceRoleList({ limit: 5 })
  selectRoleList.value = res.data
}

const getMyRoleList = async () => {
  try {
    const res = await roleApi.getMyRoleList()
    roleList.value = res.data.list || res.data
    total.value = roleList.value.length
  } catch (error) {
    console.error('获取我的角色列表失败:', error)
    ElMessage.error('获取我的角色列表失败')
    roleList.value = []
    total.value = 0
  }
}

const setViewMode = (mode: string) => {
  if (mode === 'my') {
    currentView.value = 'my'
    return
  }

  currentView.value = 'public'
  searchParam.value.orderDirection = mode === 'latest' ? 'asc' : 'desc'
}

const debouncedSearch = debounce(async () => {
  if (searchInput.value.trim()) {
    currentView.value = 'public'
    const res = await roleApi.searchRole({ keyword: searchInput.value })
    roleList.value = res.data.list
    total.value = res.data.total
    return
  }

  if (currentView.value === 'public') {
    getRoleList()
  } else {
    getMyRoleList()
  }
}, 500)

const search = () => {
  debouncedSearch()
}

const openRoleDialog = (item: roleInfo) => {
  roleSelected.value = item
  infoShow.value = true
}

onMounted(() => {
  Promise.all([getRoleList(), getSelectedRoleList()]).then(() => {})
})

const startConversation = async (characterId: string | number) => {
  try {
    if (!characterId) {
      ElMessage.error('角色信息有误，请重试')
      return
    }

    const loadingMessage = ElMessage.info('正在创建对话...')
    const conversationUuid = await chatHistoryStore().addChatHistory(characterId)

    loadingMessage.close()
    ElMessage.success('对话创建成功！')
    router.push(`/chat/${conversationUuid}`)
  } catch (error) {
    console.error('创建对话失败:', error)
    ElMessage.error('创建对话失败，请稍后重试')
  }
}

const handlePageChange = (page: number) => {
  if (page < 1 || (total.value > 0 && page > Math.ceil(total.value / searchParam.value.pageSize))) {
    return
  }
  searchParam.value.pageNum = page
}
</script>

<style lang="scss" scoped>
.discovery-page {
  display: grid;
  gap: 24px;
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
  padding: 24px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding-bottom: 8px;
}

@media (max-width: 768px) {
  .discovery-page {
    padding: 16px;
    gap: 18px;
  }
}
</style>
