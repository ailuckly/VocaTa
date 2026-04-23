<template>
  <section :class="isM ? 'mobile' : 'pc'" class="chat-page">
    <ChatStageHeader
      :avatar="characterAvatar"
      :character-name="getCharacterName()"
      :connected="isAIConnected"
      :status="connectionStatus"
      eyebrow="Companion stage"
    />

    <div class="chat-page__messages" ref="chatContainer">
      <ChatMessageList
        :messages="chats"
        :thinking="isAIThinking"
        :character-avatar="characterAvatar"
        :character-name="getCharacterName()"
        :character-initial="characterInitials"
        :user-avatar="userAvatar"
        :user-name="userDisplayName"
        :user-initial="userInitials"
        :format-time="formatTime"
      />
    </div>

    <div class="chat-page__footer">
      <VoiceCallPanel
        :active="isAudioCallActive"
        :entries="visibleVoiceTranscripts"
        :speaking="isAISpeaking"
        :muted="isMicMuted"
        :status="voiceStatusText"
        :character-name="getCharacterName()"
        :character-avatar="characterAvatar"
        @mute="toggleMicrophone"
        @hangup="stopAudioCall"
      />

      <ChatComposer
        v-model="input"
        :connected="isAIConnected"
        :recording="Boolean(aiChat?.recording)"
        @send="sendMessage"
        @toggle-call="toggleAudioCall"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { conversationApi } from '@/api/modules/conversation'
import { userApi } from '@/api/modules/user'
import { isMobile } from '@/utils/isMobile'
import { ElMessage } from 'element-plus'
import { computed, onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { ChatMessage } from '@/types/common'
import type { ConversationResponse, MessageResponse } from '@/types/api'
import { VocaTaAIChat } from '@/utils/aiChat'
import { getToken } from '@/utils/token'
import ChatComposer from '@/components/chat/ChatComposer.vue'
import ChatMessageList from '@/components/chat/ChatMessageList.vue'
import ChatStageHeader from '@/components/chat/ChatStageHeader.vue'
import VoiceCallPanel from '@/components/chat/VoiceCallPanel.vue'
import type { VoiceTranscriptItem } from '@/types/ui'

const isM = computed(() => isMobile())
const chats = ref<ChatMessage[]>([])
const isLoadingMessages = ref(false)
const hasMoreHistory = ref(true)
const currentOffset = ref(0)
const input = ref('')
const router = useRouter()
const route = useRoute()
const conversationUuid = computed(() => route.params.conversationUuid as string)
const currentConversation = ref<ConversationResponse | null>(null)

const userAvatar = ref('')
const userNickname = ref('')
const userDisplayName = computed(() => userNickname.value || '我')
const userInitials = computed(() => {
  const name = userDisplayName.value
  if (!name) return '我'
  return name.slice(0, 1).toUpperCase()
})

const aiChat = ref<VocaTaAIChat | null>(null)
const isAudioCallActive = ref(false)
const connectionStatus = ref('正在连接...')
const isAIConnected = ref(false)
const isAIThinking = ref(false)
const currentSTTText = ref('')
const currentStreamingMessage = ref<ChatMessage | null>(null)
const isAISpeaking = ref(false)

const voiceTranscripts = ref<VoiceTranscriptItem[]>([])
const hasShownGreeting = ref(false)

interface TypewriterState {
  message: ChatMessage
  targetText: string
  currentIndex: number
  intervalId: number | null
  isComplete: boolean
  started: boolean
  fallbackTimeoutId: number | null
}

const typewriterState = ref<TypewriterState | null>(null)
const TYPEWRITER_SPEED = 35
const isMicMuted = ref(false)

const chatContainer = ref<HTMLElement>()

onMounted(async () => {
  await loadUserProfile()

  if (conversationUuid.value) {
    try {
      await loadConversationAndMessages()
      if (currentConversation.value) {
        await initializeAIChat()
      }
    } catch (error) {
      console.error('❌ 页面初始化失败:', error)
      if ((error as Error).message.includes('对话不存在')) {
        router.push('/searchRole')
      }
    }
  }
})

onUnmounted(() => {
  resetTypewriterState()
  if (aiChat.value) {
    aiChat.value.destroy()
  }
})

watch(
  () => route.params.conversationUuid,
  async (newConversationUuid, oldConversationUuid) => {
    console.log('🔄 路由变化 - 旧UUID:', oldConversationUuid, '新UUID:', newConversationUuid)

    if (newConversationUuid) {
      if (aiChat.value) {
        console.log('🧹 清理之前的AI对话系统')
        aiChat.value.destroy()
        aiChat.value = null
      }

      console.log('🔄 重置所有状态')
      chats.value = []
      currentConversation.value = null
      currentOffset.value = 0
      hasMoreHistory.value = true
      isAudioCallActive.value = false
      isAIConnected.value = false
      currentSTTText.value = ''
      isAIThinking.value = false
      resetTypewriterState()
      currentStreamingMessage.value = null
      isAISpeaking.value = false
      voiceTranscripts.value = []
      hasShownGreeting.value = false

      try {
        await loadConversationAndMessages()

        if (conversationUuid.value === newConversationUuid && currentConversation.value) {
          await initializeAIChat()
        }
      } catch (error) {
        console.error('❌ 加载对话失败:', error)
        if ((error as Error).message.includes('对话不存在')) {
          router.push('/searchRole')
        }
      }
    }
  },
)

const loadUserProfile = async () => {
  try {
    const res = await userApi.getUserInfo()
    if (res.code === 200 && res.data) {
      userAvatar.value = res.data.avatar || ''
      userNickname.value = res.data.nickname || ''
    }
  } catch (error) {
    console.error('❌ 获取用户信息失败:', error)
  }
}

const loadConversationAndMessages = async () => {
  try {
    await loadConversationInfo()
    await loadRecentMessages()
  } catch (error) {
    console.error('加载对话和消息失败:', error)
  }
}

const loadRecentMessages = async (limit: number = 20) => {
  if (!conversationUuid.value || isLoadingMessages.value) return

  try {
    isLoadingMessages.value = true
    const res = await conversationApi.getRecentMessages(conversationUuid.value, limit)
    if (res.code === 200) {
      const messages = convertMessagesToChatFormat(res.data)
      chats.value = messages.reverse()
      currentOffset.value = res.data.length

      if (messages.length > 0) {
        hasShownGreeting.value = true
      } else if (!hasShownGreeting.value) {
        const greetingText = currentConversation.value?.greeting?.trim()
        if (greetingText) {
          hasShownGreeting.value = true
          chats.value = [
            {
              type: 'receive',
              content: greetingText,
              senderType: 2,
              contentType: 1,
              createDate: new Date().toISOString(),
              characterName: currentConversation.value?.characterName || getCharacterName(),
              metadata: { isGreeting: true },
            },
          ]
        }
      }

      console.log('📥 加载消息完成:', {
        messagesCount: messages.length,
        chatsLength: chats.value.length,
        hasMessages: chats.value.length > 0,
      })

      setTimeout(() => {
        scrollToBottomWithRetry()
      }, 100)
    }
  } catch (error) {
    console.error('加载消息失败:', error)
    ElMessage.error('加载消息失败')
  } finally {
    isLoadingMessages.value = false
  }
}

const convertMessagesToChatFormat = (messages: MessageResponse[]): ChatMessage[] => {
  return messages.map((msg) => ({
    messageUuid: msg.messageUuid,
    type: msg.senderType === 1 ? 'send' : 'receive',
    content: msg.textContent,
    senderType: msg.senderType,
    contentType: msg.contentType,
    audioUrl: msg.audioUrl,
    createDate: msg.createDate,
    metadata: msg.metadata,
  }))
}

const sendMessage = async () => {
  if (!input.value.trim() || !conversationUuid.value || !isAIConnected.value) return

  const messageContent = input.value.trim()
  input.value = ''

  if (aiChat.value) {
    try {
      await aiChat.value.prepareAudioPlayback()
    } catch (error) {
      console.warn('⚠️ 准备音频播放失败:', error)
    }
  }

  const userMessage: ChatMessage = {
    type: 'send',
    content: messageContent,
    senderType: 1,
    contentType: 1,
    createDate: new Date().toISOString(),
  }
  chats.value.push(userMessage)
  scrollToBottomWithRetry()
  isAIThinking.value = true

  try {
    aiChat.value?.sendTextMessage(messageContent)
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败')
    isAIThinking.value = false
    chats.value.pop()
    input.value = messageContent
  }
}

const loadConversationInfo = async () => {
  try {
    console.log('📋 加载对话信息 - UUID:', conversationUuid.value)

    const res = await conversationApi.getConversationList()
    if (res.code === 200) {
      const conversation = res.data.find((conv) => conv.conversationUuid === conversationUuid.value)

      if (!conversation) {
        console.warn('⚠️ 在对话列表中找不到当前对话UUID:', conversationUuid.value)
        ElMessage.error('对话不存在或已过期，请重新选择角色')
        throw new Error('对话不存在或已过期')
      }

      currentConversation.value = conversation
      console.log('✅ 对话信息加载完成:', {
        uuid: conversation.conversationUuid,
        title: conversation.title,
        characterName: conversation.characterName,
      })
    } else {
      throw new Error('获取对话列表失败: ' + res.message)
    }
  } catch (error) {
    console.error('❌ 获取对话信息失败:', error)
    ElMessage.error('加载对话信息失败，请刷新页面重试')
    throw error
  }
}

const getCharacterName = () => {
  return currentConversation.value?.characterName || 'AI助手'
}

const characterAvatar = computed(() => currentConversation.value?.characterAvatarUrl || '')

const characterInitials = computed(() => {
  const name = getCharacterName()
  if (!name) return 'AI'
  return name.slice(0, 2).toUpperCase()
})

const voiceStatusText = computed(() => {
  if (!isAIConnected.value) return '语音通道连接中…'
  if (isMicMuted.value) return '麦克风已静音'
  if (aiChat.value?.recording) return '正在聆听...'
  if (isAISpeaking.value) return 'AI 回答中'
  if (isAIThinking.value) return 'AI 正在思考…'
  return '语音对话中'
})

const visibleVoiceTranscripts = computed(() => {
  const list = [...voiceTranscripts.value]
  
  if (currentSTTText.value) {
    list.push({
      speaker: 'user',
      text: currentSTTText.value,
      timestamp: Number.MAX_SAFE_INTEGER - 1
    })
  }
  
  if (currentStreamingMessage.value && currentStreamingMessage.value.content) {
    list.push({
      speaker: 'ai',
      text: currentStreamingMessage.value.content,
      timestamp: Number.MAX_SAFE_INTEGER
    })
  }

  return list.slice(-3)
})

const initializeAIChat = async () => {
  console.log('🔥 开始初始化AI对话系统...')
  try {
    if (!conversationUuid.value) {
      throw new Error('对话UUID不能为空')
    }

    const token = getToken()
    if (!token) {
      throw new Error('用户未登录')
    }

    console.log('🚀 初始化AI对话系统 - conversationUuid:', conversationUuid.value)
    connectionStatus.value = '正在连接AI系统...'

    aiChat.value = new VocaTaAIChat()
    setupAIChatCallbacks()
    await aiChat.value.initialize(conversationUuid.value)

    console.log('✅ AI对话系统初始化完成！')
  } catch (error) {
    console.error('❌ 初始化AI对话系统失败:', error)
    connectionStatus.value = '连接失败，请刷新页面重试'
    ElMessage.error('AI对话系统初始化失败: ' + (error as Error).message)
  }
}

const setupAIChatCallbacks = () => {
  if (!aiChat.value) return

  aiChat.value.onConnectionStatus((status) => {
    switch (status) {
      case 'connected':
        connectionStatus.value = '已连接到AI服务'
        isAIConnected.value = true
        break
      case 'disconnected':
        connectionStatus.value = '连接已断开，正在重连...'
        isAIConnected.value = false
        break
      case 'error':
        connectionStatus.value = '连接失败'
        isAIConnected.value = false
        break
    }
  })

  aiChat.value.onSTTResult((text, isFinal) => {
    currentSTTText.value = text

    if (isFinal) {
      const userMessage: ChatMessage = {
        type: 'send',
        content: text,
        senderType: 1,
        contentType: 2,
        createDate: new Date().toISOString(),
      }
      chats.value.push(userMessage)
      voiceTranscripts.value.push({
        speaker: 'user',
        text,
        timestamp: Date.now(),
      })
      if (voiceTranscripts.value.length > 12) {
        voiceTranscripts.value.splice(0, voiceTranscripts.value.length - 12)
      }
      scrollToBottomWithRetry()
      isAIThinking.value = true
      currentSTTText.value = ''
    }
  })

  aiChat.value.onLLMStream((text, isComplete, characterName) => {
    console.log('🤖 收到LLM流式消息:', {
      text: text?.substring(0, 50),
      textLength: text?.length,
      isComplete,
      characterName,
      currentStreamingExists: !!currentStreamingMessage.value,
    })

    const content = text ?? ''
    const trimmed = content.trim()
    const previousState = typewriterState.value
    const wasComplete = previousState?.isComplete ?? false

    if (!trimmed) {
      if (isComplete) {
        isAIThinking.value = false
        if (previousState) {
          previousState.isComplete = true
          if (!previousState.started) {
            scheduleTypewriterFallback()
          }
        } else if (currentStreamingMessage.value) {
          currentStreamingMessage.value.isStreaming = false
          currentStreamingMessage.value = null
        }
      }
      return
    }

    let state = typewriterState.value

    if (!currentStreamingMessage.value) {
      const newMessage: ChatMessage = {
        type: 'receive',
        content: '',
        senderType: 2,
        contentType: 1,
        createDate: new Date().toISOString(),
        isStreaming: true,
        characterName: characterName || getCharacterName(),
      }

      chats.value.push(newMessage)
      currentStreamingMessage.value = newMessage
      state = {
        message: newMessage,
        targetText: content,
        currentIndex: 0,
        intervalId: null,
        isComplete,
        started: false,
        fallbackTimeoutId: null,
      }
      typewriterState.value = state
      isAIThinking.value = false
      scrollToBottomWithRetry()
    } else {
      currentStreamingMessage.value.isStreaming = true
      if (characterName) {
        currentStreamingMessage.value.characterName = characterName
      }

      if (!state || state.message !== currentStreamingMessage.value) {
        state = {
          message: currentStreamingMessage.value,
          targetText: content,
          currentIndex: currentStreamingMessage.value.content.length,
          intervalId: state?.intervalId ?? null,
          isComplete: state?.isComplete ?? false,
          started: state?.started ?? false,
          fallbackTimeoutId: state?.fallbackTimeoutId ?? null,
        }
        typewriterState.value = state
      }
    }

    state = typewriterState.value
    if (!state) return

    state.targetText = content
    state.isComplete = isComplete
    state.currentIndex = Math.min(state.currentIndex, state.targetText.length)
    if (!state.started) {
      state.message.content = state.targetText.slice(0, state.currentIndex)
    }
    state.message.isStreaming = true

    if (isComplete && !wasComplete && trimmed) {
      voiceTranscripts.value.push({
        speaker: 'ai',
        text: trimmed,
        timestamp: Date.now(),
      })
      if (voiceTranscripts.value.length > 12) {
        voiceTranscripts.value.splice(0, voiceTranscripts.value.length - 12)
      }
    }

    if (isAISpeaking.value && !state.started) {
      startTypewriterEffect()
    }

    if (isComplete) {
      scheduleTypewriterFallback()
    }
  })

  aiChat.value.onAudioPlay((isPlaying) => {
    console.log('🔊 音频播放状态:', isPlaying)
    isAISpeaking.value = isPlaying

    if (isPlaying) {
      startTypewriterEffect()
    } else if (typewriterState.value) {
      if (!typewriterState.value.started) {
        scheduleTypewriterFallback()
      } else if (typewriterState.value.isComplete) {
        finalizeTypewriter()
      }
    }
  })
}

const toggleAudioCall = async () => {
  if (isAudioCallActive.value) {
    await stopAudioCall()
  } else {
    await startAudioCall()
  }
}

const startAudioCall = async () => {
  try {
    if (!aiChat.value) {
      ElMessage.error('AI对话系统未初始化')
      return
    }

    console.log('📞 开始音频通话')
    await aiChat.value.prepareAudioPlayback()
    await aiChat.value.startAudioCall()
    isAudioCallActive.value = true
    isMicMuted.value = false
    voiceTranscripts.value = []
  } catch (error) {
    console.error('❌ 启动音频通话失败:', error)
    ElMessage.error('无法启动音频通话: ' + (error as Error).message)
  }
}

const stopAudioCall = async () => {
  try {
    if (!aiChat.value) return

    console.log('📞 停止音频通话')
    await aiChat.value.stopAudioCall()
    isAudioCallActive.value = false
    isMicMuted.value = false
    currentSTTText.value = ''
    isAISpeaking.value = false
  } catch (error) {
    console.error('❌ 停止音频通话失败:', error)
    ElMessage.error('停止音频通话失败: ' + (error as Error).message)
  }
}

const toggleMicrophone = () => {
  if (!aiChat.value || !isAudioCallActive.value) return

  if (isMicMuted.value) {
    aiChat.value.unmuteMic()
    isMicMuted.value = false
  } else {
    aiChat.value.muteMic()
    isMicMuted.value = true
  }
}

const clearTypewriterTimers = () => {
  const state = typewriterState.value
  if (!state) return

  if (state.intervalId !== null) {
    window.clearInterval(state.intervalId)
    state.intervalId = null
  }

  if (state.fallbackTimeoutId !== null) {
    window.clearTimeout(state.fallbackTimeoutId)
    state.fallbackTimeoutId = null
  }
}

const finalizeTypewriter = () => {
  const state = typewriterState.value
  if (!state) return

  clearTypewriterTimers()
  state.message.content = state.targetText
  state.message.isStreaming = false
  isAIThinking.value = false
  typewriterState.value = null
  currentStreamingMessage.value = null
  scrollToBottomWithRetry()
}

const startTypewriterEffect = () => {
  const state = typewriterState.value
  if (!state || state.started) return

  clearTypewriterTimers()
  state.started = true
  state.message.isStreaming = true
  state.intervalId = window.setInterval(() => {
    const targetLength = state.targetText.length
    if (state.currentIndex < targetLength) {
      state.currentIndex += 1
      state.message.content = state.targetText.slice(0, state.currentIndex)
      scrollToBottomWithRetry()
    } else if (state.isComplete) {
      finalizeTypewriter()
    }
  }, TYPEWRITER_SPEED)
}

const scheduleTypewriterFallback = () => {
  const state = typewriterState.value
  if (!state || state.started) return

  if (state.fallbackTimeoutId !== null) return

  state.fallbackTimeoutId = window.setTimeout(() => {
    state.fallbackTimeoutId = null
    startTypewriterEffect()
  }, 500)
}

const resetTypewriterState = () => {
  const state = typewriterState.value
  if (!state) return

  clearTypewriterTimers()
  state.message.isStreaming = false
  isAIThinking.value = false
  typewriterState.value = null
  currentStreamingMessage.value = null
}

const scrollToBottomWithRetry = (maxRetries: number = 3) => {
  let retries = 0

  const attempt = () => {
    try {
      if (chatContainer.value) {
        const container = chatContainer.value
        const isScrollable = container.scrollHeight > container.clientHeight

        console.log('📜 滚动信息:', {
          scrollHeight: container.scrollHeight,
          clientHeight: container.clientHeight,
          scrollTop: container.scrollTop,
          isScrollable,
          retries,
        })

        if (isScrollable) {
          container.scrollTo({
            top: container.scrollHeight,
            behavior: 'smooth',
          })

          container.scrollTop = container.scrollHeight
          console.log('✅ 滚动完成，新scrollTop:', container.scrollTop)
        } else {
          console.log('📜 容器不需要滚动')
        }
      } else {
        console.warn('⚠️ chatContainer引用为空')
      }
    } catch (error) {
      console.error('❌ 滚动操作失败:', error)
    }
  }

  const tryScroll = () => {
    attempt()

    if (retries < maxRetries) {
      retries++
      setTimeout(() => {
        if (chatContainer.value) {
          const isAtBottom =
            Math.abs(
              chatContainer.value.scrollHeight -
                chatContainer.value.scrollTop -
                chatContainer.value.clientHeight,
            ) <= 5

          if (!isAtBottom) {
            console.log(`🔄 滚动重试 ${retries}/${maxRetries}`)
            tryScroll()
          }
        }
      }, 100)
    }
  }

  nextTick(() => {
    tryScroll()
  })
}

const formatTime = (dateString: string) => {
  return new Date(dateString).toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style lang="scss" scoped>
.chat-page {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 12px;
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
  height: 100%;
  min-height: 0;
  padding: 8px 0 12px;
  overflow: hidden;
}

.chat-page__messages {
  min-height: 0;
  height: 100%;
  padding: 12px 0 8px;
  overflow-y: auto;
  background: transparent;
  scroll-behavior: smooth;
}

.chat-page__footer {
  display: grid;
  gap: 10px;
}

.chat-page :deep(.chat-composer) {
  position: sticky;
  bottom: 0;
  z-index: 10;
  padding-top: 8px;
  background:
    linear-gradient(180deg, rgba(241, 251, 250, 0) 0%, rgba(241, 251, 250, 0.92) 18%, rgba(241, 251, 250, 1) 100%);
}

@media screen and (max-width: 768px) {
  .chat-page {
    padding-bottom: 12px;
  }

  .chat-page__messages {
    padding: 8px 0;
  }
}
</style>
