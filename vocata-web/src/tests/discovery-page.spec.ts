import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import SearchRole from '@/views/SearchRole.vue'

const routerPush = vi.hoisted(() => vi.fn())
const roleApiMocks = vi.hoisted(() => ({
  getPublicRoleList: vi.fn(),
  getChoiceRoleList: vi.fn(),
}))
const addChatHistory = vi.hoisted(() => vi.fn())

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return {
    ...actual,
    useRouter: () => ({ push: routerPush }),
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
    chatHistory: [],
    getChatHistory: vi.fn().mockResolvedValue(undefined),
    addChatHistory,
  }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    info: vi.fn(() => ({ close: vi.fn() })),
    error: vi.fn(),
  },
}))

const publicRoles = [
  {
    id: 1,
    name: '猫耳少女',
    description: '可爱黏人的陪伴系角色',
    greeting: '今天也想陪着你。',
    avatarUrl: '/avatars/alice.png',
    tags: ['恋爱陪伴', '二次元'],
    chatCount: 214000,
  },
  {
    id: 2,
    name: '冷淡执事',
    description: '克制、优雅、只听命于你',
    avatarUrl: '/avatars/holmes.png',
    tags: ['角色扮演', '高冷'],
    chatCount: 168000,
  },
  {
    id: 3,
    name: '赛博女友',
    description: '来自未来的专属恋人',
    avatarUrl: '/avatars/athena.png',
    tags: ['恋爱陪伴', '未来科幻'],
    chatCount: 152000,
  },
  {
    id: 4,
    name: '古风公子',
    description: '执笔为你写尽风雅',
    avatarUrl: '/avatars/libai.png',
    tags: ['古风', '治愈陪伴'],
    chatCount: 126000,
  },
]

const featuredRoles = [
  {
    id: 9,
    name: '哈利·波特',
    greeting: '霍格沃茨的夜晚刚刚开始。想和我一起探索禁林、学习咒语，还是聊聊你的烦恼？',
    description: '魔法校园冒险',
    avatarUrl: '/avatars/harry.png',
    tags: ['魔法', '冒险', '勇气', '校园'],
    chatCount: 289000,
  },
  {
    id: 10,
    name: '苏格拉底',
    greeting: '我们从一个问题开始。',
    avatarUrl: '/avatars/socrates.png',
    tags: ['深夜树洞'],
    chatCount: 124000,
  },
]

const flushView = async () => {
  await Promise.resolve()
  await Promise.resolve()
}

describe('SearchRole commercial home', () => {
  it('renders a content-first role plaza with entertainment categories and discovery modules', async () => {
    roleApiMocks.getPublicRoleList.mockResolvedValue({
      code: 200,
      data: { list: publicRoles, total: publicRoles.length },
    })
    roleApiMocks.getChoiceRoleList.mockResolvedValue({
      code: 200,
      data: featuredRoles,
    })

    const wrapper = mount(SearchRole, {
      global: {
        stubs: {
          RoleDialog: true,
          Transition: false,
          RouterLink: {
            props: ['to'],
            template: '<a :href="typeof to === `string` ? to : `#`"><slot /></a>',
          },
        },
      },
    })

    await flushView()

    expect(roleApiMocks.getChoiceRoleList).toHaveBeenCalledWith({ limit: 6 })
    expect(roleApiMocks.getPublicRoleList).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 16,
      orderDirection: 'desc',
      tags: undefined,
    })
    expect(wrapper.text()).toContain('哈利·波特')
    expect(wrapper.text()).toContain('立即体验')
    expect(wrapper.text()).toContain('恋爱陪伴')
    expect(wrapper.text()).toContain('二次元')
    expect(wrapper.text()).toContain('深夜树洞')
    expect(wrapper.text()).toContain('编辑精选')
    expect(wrapper.text()).toContain('热门角色')
    expect(wrapper.text()).toContain('猜你喜欢')
    expect(wrapper.text()).toContain('命运交错的那一天')
    expect(wrapper.text()).toContain('学院怪谈事件簿')
    expect(wrapper.text()).toContain('# 病娇')
    expect(wrapper.text()).not.toContain('# 哲学')
    expect(wrapper.findAll('[data-test="creator-avatar"] img')).toHaveLength(3)
  })

  it('keeps chat conversion actions wired without changing role list parameters', async () => {
    roleApiMocks.getPublicRoleList.mockResolvedValue({
      code: 200,
      data: { list: publicRoles, total: publicRoles.length },
    })
    roleApiMocks.getChoiceRoleList.mockResolvedValue({
      code: 200,
      data: featuredRoles,
    })
    addChatHistory.mockResolvedValue('conv-hero')

    const wrapper = mount(SearchRole, {
      global: {
        stubs: {
          RoleDialog: true,
          Transition: false,
          RouterLink: {
            props: ['to'],
            template: '<a :href="typeof to === `string` ? to : `#`"><slot /></a>',
          },
        },
      },
    })

    await flushView()
    await wrapper.find('[data-test="hero-chat"]').trigger('click')
    await flushView()

    expect(addChatHistory).toHaveBeenCalledWith(9)
    expect(routerPush).toHaveBeenCalledWith('/chat/conv-hero')

    roleApiMocks.getPublicRoleList.mockClear()
    await wrapper.find('[data-test="category-chip-恋爱陪伴"]').trigger('click')
    await flushView()

    expect(roleApiMocks.getPublicRoleList).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 16,
      orderDirection: 'desc',
      tags: ['恋爱陪伴'],
    })
  })
})
