import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import SearchRole from '@/views/SearchRole.vue'

const roleApiMocks = vi.hoisted(() => ({
  getPublicRoleList: vi.fn(),
  getChoiceRoleList: vi.fn(),
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return {
    ...actual,
    useRouter: () => ({ push: vi.fn() }),
  }
})

vi.mock('@/api/modules/role', () => ({
  roleApi: {
    getPublicRoleList: roleApiMocks.getPublicRoleList,
    getChoiceRoleList: roleApiMocks.getChoiceRoleList,
    getMyRoleList: vi.fn(),
    searchRole: vi.fn(),
  },
}))

vi.mock('@/store', () => ({
  chatHistoryStore: () => ({
    addChatHistory: vi.fn().mockResolvedValue('conv-1'),
  }),
}))

const flushView = async () => {
  await Promise.resolve()
  await Promise.resolve()
}

describe('SearchRole', () => {
  it('loads featured and public roles on mount', async () => {
    roleApiMocks.getPublicRoleList.mockResolvedValue({
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
    })
    roleApiMocks.getChoiceRoleList.mockResolvedValue({
      code: 200,
      data: [
        {
          id: 9,
          name: '精选角色',
          greeting: '欢迎',
          description: 'desc',
          avatarUrl: '',
          chatCount: 99,
        },
      ],
    })

    const wrapper = mount(SearchRole, {
      global: {
        stubs: {
          RoleDialog: true,
          Transition: false,
        },
      },
    })

    await flushView()

    expect(roleApiMocks.getChoiceRoleList).toHaveBeenCalledWith({ limit: 6 })
    expect(roleApiMocks.getPublicRoleList).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 15,
      orderDirection: 'desc',
      tags: undefined,
    })
    expect(wrapper.text()).toContain('欢迎来到 VocaTa')
    expect(wrapper.text()).toContain('晚安电台')
  })

  it('reloads public roles with selected tag', async () => {
    roleApiMocks.getPublicRoleList.mockResolvedValue({
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
    })
    roleApiMocks.getChoiceRoleList.mockResolvedValue({ code: 200, data: [] })

    const wrapper = mount(SearchRole, {
      global: {
        stubs: {
          RoleDialog: true,
          Transition: false,
        },
      },
    })

    await flushView()
    roleApiMocks.getPublicRoleList.mockClear()

    const tabButtons = wrapper.findAll('.category-tabs__item')
    await tabButtons[5].trigger('click')
    await flushView()

    expect(roleApiMocks.getPublicRoleList).toHaveBeenCalledTimes(1)
    expect(roleApiMocks.getPublicRoleList).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 15,
      orderDirection: 'desc',
      tags: ['动漫'],
    })
  })
})
