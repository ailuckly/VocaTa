import type { roleInfo } from '@/types/common'

export interface HomeRoleCardData {
  id: number
  name: string
  desc: string
  heat: string
  avatar: string
}

export interface FeaturedCardData {
  id: number
  name: string
  tags: string[]
  count: string
  cover: string
}

export interface ContinueChatData {
  id: number | string
  name: string
  lastTopic: string
  avatar: string
}

export interface TopicData {
  title: string
  count: string
  cover: string
}

export interface CreatorData {
  name: string
  skill: string
  fans: string
  avatar: string
}

export const CATEGORY_CHIPS = [
  '推荐',
  '恋爱陪伴',
  '二次元',
  '角色扮演',
  '剧情冒险',
  '深夜树洞',
  '治愈陪伴',
  '校园日常',
  '更多',
]

export const HOT_TAGS = [
  '恋爱陪伴',
  '病娇',
  '先婚后爱',
  '双向奔赴',
  '救赎',
  '校园',
  '异世界',
  '高冷',
]

export const CONTINUE_CHATS: ContinueChatData[] = [
  { id: 31, name: '汐海觅舟', lastTopic: '深海航线', avatar: '/avatars/namiya.png' },
  { id: 32, name: '雾中回响', lastTopic: '迷途侦探', avatar: '/avatars/holmes.png' },
  { id: 33, name: '契约玫瑰', lastTopic: '蔷薇誓约', avatar: '/avatars/zhenhuan.png' },
  { id: 34, name: '星辰守墓人', lastTopic: '记忆碑铭', avatar: '/avatars/athena.png' },
]

export const FEATURED_CARDS: FeaturedCardData[] = [
  { id: 41, name: '命运交错的那一天', tags: ['命运的齿轮开始转动...'], count: '28.9万', cover: '/avatars/storydice.png' },
  { id: 42, name: '学院怪谈事件簿', tags: ['夜半钟声，旧楼传闻...'], count: '22.1万', cover: '/avatars/goodnight.png' },
  { id: 43, name: '限时心动企划', tags: ['心动倒计时，开始！'], count: '19.8万', cover: '/avatars/affirm.png' },
  { id: 44, name: '异世界契约冒险', tags: ['穿越异界，签订契约！'], count: '16.4万', cover: '/avatars/worldbuild.png' },
]

export const HOT_ROLES: HomeRoleCardData[] = [
  { id: 51, name: '路西恩', desc: '吸血贵族', heat: '21.4万', avatar: '/avatars/villain.png' },
  { id: 52, name: '白樱纪年', desc: '时空旅者', heat: '16.8万', avatar: '/avatars/alice.png' },
  { id: 53, name: '沈砚舟', desc: '腹黑医生', heat: '15.2万', avatar: '/avatars/holmes.png' },
  { id: 54, name: '洛璃', desc: '星界守护', heat: '12.6万', avatar: '/avatars/athena.png' },
]

export const RECOMMENDED_ROLES: HomeRoleCardData[] = [
  { id: 61, name: '千夜落', desc: '神秘花店', heat: '17.4万', avatar: '/avatars/diary.png' },
  { id: 62, name: '未央九歌', desc: '古风玄幻', heat: '15.6万', avatar: '/avatars/zhuangzi.png' },
  { id: 63, name: '蒸汽心跳', desc: '未来科幻', heat: '13.1万', avatar: '/avatars/ironman.png' },
  { id: 64, name: '林深时见鹿', desc: '校园日常', heat: '11.2万', avatar: '/avatars/littleprince.png' },
]

export const TOPICS: TopicData[] = [
  { title: '命运交错的那一天', count: '28.9万', cover: '/avatars/storydice.png' },
  { title: '学院怪谈事件簿', count: '22.1万', cover: '/avatars/goodnight.png' },
  { title: '限时心动企划', count: '19.8万', cover: '/avatars/affirm.png' },
  { title: '异世界契约冒险', count: '16.4万', cover: '/avatars/worldbuild.png' },
]

export const CREATORS: CreatorData[] = [
  { name: '鹿鸣', skill: '恋爱 · 治愈', fans: '12.3万', avatar: '/avatars/alice.png' },
  { name: '北川听风', skill: '古风 · 剧情', fans: '9.8万', avatar: '/avatars/libai.png' },
  { name: '云端漫游者', skill: '异世界 · 冒险', fans: '15.6万', avatar: '/avatars/worldbuild.png' },
]

export function roleToCard(role: roleInfo): HomeRoleCardData {
  return {
    id: role.id,
    name: role.name || '未命名角色',
    desc: role.description || role.greeting || '适合立刻开口的陪伴角色',
    heat: formatHeat(role.chatCount),
    avatar: role.avatarUrl || '/avatars/dialogue.png',
  }
}

export function formatHeat(count?: number) {
  if (!count) return '0'
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万`
  return count.toLocaleString()
}
