import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import SearchRole from '@/views/SearchRole.vue'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return {
    ...actual,
    useRouter: () => ({ push: vi.fn() }),
  }
})

vi.mock('@/api/modules/role', () => ({
  roleApi: {
    getPublicRoleList: vi.fn().mockResolvedValue({
      code: 200,
      data: {
        list: [
          {
            id: 1,
            name: '晚安电台',
            greeting: '晚安好呀，今晚想聊什么？',
            description: '陪你把今天慢慢讲完。',
            avatarUrl: '',
            chatCount: 12,
          },
        ],
        total: 1,
      },
    }),
    getChoiceRoleList: vi.fn().mockResolvedValue({
      code: 200,
      data: [
        {
          id: 1,
          name: '晚安电台',
          greeting: '晚安好呀，今晚想聊什么？',
          description: '陪你把今天慢慢讲完。',
          avatarUrl: '',
          chatCount: 12,
        },
      ],
    }),
    getMyRoleList: vi.fn().mockResolvedValue({
      code: 200,
      data: { list: [], total: 0 },
    }),
    searchRole: vi.fn().mockResolvedValue({
      code: 200,
      data: { list: [], total: 0 },
    }),
  },
}))

vi.mock('@/store', () => ({
  chatHistoryStore: () => ({
    addChatHistory: vi.fn().mockResolvedValue('conv-1'),
  }),
}))

describe('SearchRole', () => {
  it('renders the foyer hero and filter landmarks', () => {
    const wrapper = mount(SearchRole, {
      global: {
        stubs: {
          'el-pagination': true,
          'el-icon': {
            template: '<span><slot /></span>',
          },
        },
      },
    })

    expect(wrapper.find('[data-test="discovery-hero"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="role-filter-bar"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="role-shelf"]').exists()).toBe(true)
  })

  it('renders role cards with stable layout hooks', async () => {
    const wrapper = mount(SearchRole, {
      global: {
        stubs: {
          'el-pagination': true,
          'el-icon': {
            template: '<span><slot /></span>',
          },
        },
      },
    })

    await new Promise((resolve) => setTimeout(resolve, 0))

    const card = wrapper.find('[data-test="role-card"]')
    expect(card.exists()).toBe(true)
    expect(wrapper.find('[data-test="role-card-front"]').exists()).toBe(true)
    expect(card.text()).toContain('开始对话')
  })
})
