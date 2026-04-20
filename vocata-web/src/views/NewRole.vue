<template>
  <section :class="isM ? 'mobile' : 'pc'" class="creator-page">
    <div class="creator-page__content">
      <header class="creator-page__header">
        <p>Create a companion</p>
        <h1>把角色设定、声音和口吻组织成一个可聊天的陪伴对象。</h1>
      </header>

      <div class="creator-page__grid">
        <div class="creator-page__form">
          <CreatorIdentitySection
            v-model="form"
            v-model:image-url="imageUrl"
            :upload-action="baseUrl + '/api/client/character/upload-avatar'"
            :headers="{ Authorization: 'Bearer ' + getToken() }"
            :before-upload="beforeAvatarUpload"
          />
          <CreatorVoiceSection
            :voice-id="form.voiceId"
            :is-public="form.isPublic"
            :options="options"
            @update:voice-id="form.voiceId = $event"
            @update:is-public="form.isPublic = $event"
          />
          <CreatorPersonaSection
            :persona="form.persona"
            :is-generating="isGenerating"
            @update:persona="form.persona = $event"
            @generate="generatePrompt"
          />
          <button class="creator-page__submit" type="button" @click="createRole">创建并开始对话</button>
        </div>

        <CreatorPreviewCard
          :name="form.name"
          :greeting="form.greeting"
          :description="form.description"
          :avatar-url="form.avatarUrl"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import CreatorIdentitySection from '@/components/creator/CreatorIdentitySection.vue'
import CreatorPersonaSection from '@/components/creator/CreatorPersonaSection.vue'
import CreatorPreviewCard from '@/components/creator/CreatorPreviewCard.vue'
import CreatorVoiceSection from '@/components/creator/CreatorVoiceSection.vue'
import { roleApi } from '@/api/modules/role'
import { chatHistoryStore } from '@/store'
import type { CreateCharacterRequest, TtsVoiceOption } from '@/types/api'
import { isMobile } from '@/utils/isMobile'
import { getToken } from '@/utils/token'
import { ElMessage } from 'element-plus'
import type { UploadProps } from 'element-plus'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const isM = computed(() => isMobile())
const baseUrl = import.meta.env.VITE_APP_URL
const imageUrl = ref('')
const isGenerating = ref(false)
const form = ref<CreateCharacterRequest>({
  name: '',
  description: '',
  greeting: '',
  isPublic: false,
  persona: '',
  voiceId: '',
  avatarUrl: '',
})

const options = ref<TtsVoiceOption[]>([])
const router = useRouter()

const getVoice = async () => {
  const res = await roleApi.getSoundList()
  options.value = res.data
}
getVoice()

const beforeAvatarUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('头像必须是JPG或PNG格式!')
    return false
  } else if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('头像大小不能超过2MB!')
    return false
  }
  return true
}

const generatePrompt = async () => {
  if (!form.value.name.trim()) {
    ElMessage.error('请先填写角色名称')
    return
  }
  if (!form.value.description.trim()) {
    ElMessage.error('请先填写角色描述')
    return
  }
  if (!form.value.greeting.trim()) {
    ElMessage.error('请先填写开场白')
    return
  }

  try {
    isGenerating.value = true
    ElMessage.info('正在生成角色提示词，请稍候...')

    const res = await roleApi.aiGenerate({
      name: form.value.name,
      description: form.value.description,
      greeting: form.value.greeting,
    })

    if (res.code === 200) {
      form.value.persona = res.data.persona
      ElMessage.success('AI生成完成！')
    } else {
      ElMessage.error(res.message || 'AI生成失败，请重试')
    }
  } catch (error) {
    console.error('AI生成出错:', error)
    ElMessage.error('AI生成出错，请检查网络连接后重试')
  } finally {
    isGenerating.value = false
  }
}

const createRole = async () => {
  if (form.value.name === '') {
    ElMessage.error('请输入角色名称')
    return
  }
  if (form.value.description === '') {
    ElMessage.error('请输入角色描述')
    return
  }
  if (form.value.greeting === '') {
    ElMessage.error('请输入角色开场白')
    return
  }
  if (form.value.voiceId === '') {
    ElMessage.error('请选择角色声音')
    return
  }

  const res = await roleApi.createRole(form.value)
  if (res.code === 200) {
    ElMessage.success('创建成功')
  } else {
    ElMessage.error(res.message)
  }
  startConversation(res.data.id)
}

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
</script>

<style lang="scss" scoped>
.creator-page {
  width: 100%;
  padding: 24px;
}

.creator-page__content {
  display: grid;
  gap: 24px;
}

.creator-page__header {
  display: grid;
  gap: 10px;
}

.creator-page__header p,
.creator-page__header h1 {
  margin: 0;
}

.creator-page__header p {
  color: var(--vt-text-soft);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 12px;
}

.creator-page__header h1 {
  max-width: 14ch;
  font-size: clamp(36px, 4vw, 58px);
  line-height: 1.04;
}

.creator-page__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
  gap: 24px;
  align-items: start;
}

.creator-page__form {
  display: grid;
  gap: 18px;
}

.creator-page__submit {
  justify-self: start;
  border: 0;
  border-radius: 999px;
  padding: 12px 18px;
  background: var(--vt-brand);
  color: white;
  cursor: pointer;
}

@media (max-width: 960px) {
  .creator-page {
    padding: 16px;
  }

  .creator-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
