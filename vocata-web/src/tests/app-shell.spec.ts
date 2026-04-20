import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '@/App.vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import LoginPage from '@/views/LoginPage.vue'
import NewRole from '@/views/NewRole.vue'
import ProfilePage from '@/views/ProfilePage.vue'
import routes from '@/router/routes'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return {
    ...actual,
    useRouter: () => ({ push: vi.fn() }),
    useRoute: () => ({ path: '/searchRole', params: {} }),
  }
})

vi.mock('@/api/modules/user', () => ({
  userApi: {
    getUserInfo: vi.fn().mockResolvedValue({
      code: 200,
      data: {
        nickname: '测试用户',
        avatar: '',
      },
    }),
    login: vi.fn(),
    register: vi.fn(),
    sendCode: vi.fn(),
  },
}))

vi.mock('@/api/modules/role', () => ({
  roleApi: {
    getSoundList: vi.fn().mockResolvedValue({
      code: 200,
      data: [{ name: '温柔女声' }],
    }),
    aiGenerate: vi.fn(),
    createRole: vi.fn(),
  },
}))

vi.mock('@/store', () => ({
  chatHistoryStore: () => ({
    chatHistory: [],
    addChatHistory: vi.fn(),
    getChatHistory: vi.fn().mockResolvedValue(undefined),
  }),
}))

describe('App theme shell', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the app root with companion theme class hooks', () => {
    const wrapper = mount(App, {
      global: {
        stubs: {
          RouterView: { template: '<div data-test="router-view" />' },
        },
      },
    })

    expect(wrapper.find('[data-test="app-shell"]').exists()).toBe(true)
    expect(wrapper.classes()).toContain('vocata-app')
  })

  it('renders companion navigation landmarks', () => {
    const wrapper = mount(BasicLayout, {
      global: {
        stubs: {
          AppSidebar: { template: '<aside data-test="app-sidebar"></aside>' },
          RouterView: { template: '<div data-test="route-slot" />' },
        },
      },
    })

    expect(wrapper.find('[data-test="app-sidebar"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="app-main"]').exists()).toBe(true)
  })

  it('registers the profile page route', () => {
    const profileRoute = JSON.stringify(routes)

    expect(profileRoute).toContain('/profile')
  })

  it('renders the branded login entry copy', () => {
    const wrapper = mount(LoginPage, {
      global: {
        stubs: {
          'el-form': true,
          'el-input': true,
          'el-checkbox': true,
          'el-button': true,
        },
      },
    })

    expect(wrapper.text()).toContain('把每一次对话留在更舒服的空间里')
  })

  it('renders the creator workbench sections', () => {
    const wrapper = mount(NewRole, {
      global: {
        stubs: {
          'el-upload': true,
          'el-select': true,
          'el-option': true,
          'el-checkbox': true,
          'el-form': true,
          'el-icon': true,
        },
      },
    })

    expect(wrapper.find('[data-test="creator-identity"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="creator-persona"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="creator-preview"]').exists()).toBe(true)
  })

  it('renders the profile relationship sections', () => {
    const wrapper = mount(ProfilePage)

    expect(wrapper.find('[data-test="profile-overview"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="profile-recents"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="profile-favorites"]').exists()).toBe(true)
  })
})
