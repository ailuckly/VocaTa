import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import ChatPage from '@/views/ChatPage.vue'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return {
    ...actual,
    useRoute: () => ({ params: { conversationUuid: 'conv-1' } }),
    useRouter: () => ({ push: vi.fn() }),
  }
})

vi.mock('@/api/modules/user', () => ({
  userApi: {
    getUserInfo: vi.fn().mockResolvedValue({
      code: 200,
      data: {
        avatar: '',
        nickname: '测试用户',
      },
    }),
  },
}))

vi.mock('@/api/modules/conversation', () => ({
  conversationApi: {
    getConversationList: vi.fn().mockResolvedValue({
      code: 200,
      data: [
        {
          conversationUuid: 'conv-1',
          characterId: 'role-1',
          characterName: '测试角色',
          characterAvatarUrl: '',
          greeting: '你好',
          title: '测试对话',
          lastMessageSummary: null,
          status: 1,
          createDate: new Date().toISOString(),
          updateDate: new Date().toISOString(),
        },
      ],
    }),
    getRecentMessages: vi.fn().mockResolvedValue({
      code: 200,
      data: [],
    }),
  },
}))

vi.mock('@/utils/aiChat', () => ({
  VocaTaAIChat: class {
    recording = false
    async initialize() {}
    destroy() {}
    onConnectionStatus() {}
    onSTTResult() {}
    onLLMStream() {}
    onTTSAudioStart() {}
    onTTSAudioEnd() {}
    onTTSAudioChunk() {}
    onError() {}
    onRecordingStateChange() {}
    onUserInterruption() {}
    onAudioPlay() {}
    async prepareAudioPlayback() {}
    sendTextMessage() {}
    async startAudioCall() {}
    async stopAudioCall() {}
    startRecording() {}
    stopRecording() {}
    setMuted() {}
    muteMic() {}
    unmuteMic() {}
  },
}))

vi.mock('@/utils/token', () => ({
  getToken: () => 'test-token',
}))

describe('ChatPage', () => {
  it('renders the companion chat landmarks', () => {
    const wrapper = mount(ChatPage, {
      global: {
        stubs: {
          'el-input': {
            template: '<textarea />',
          },
          'el-icon': {
            template: '<span><slot /></span>',
          },
        },
      },
    })

    expect(wrapper.find('[data-test="chat-stage-header"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="chat-composer"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="voice-panel"]').exists()).toBe(true)
  })
})
