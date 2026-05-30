import { describe, expect, it, vi } from 'vitest'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
  },
}))

vi.mock('@/utils/token', () => ({
  getToken: () => '',
  removeToken: vi.fn(),
}))

vi.mock('@/router', () => ({
  default: {
    push: vi.fn(),
  },
}))

import request from '@/api/request'

describe('request params serialization', () => {
  it('serializes tag arrays without bracket suffixes', () => {
    const uri = request.getUri({
      url: '/api/open/character/list',
      params: {
        pageNum: 1,
        tags: ['动漫', '科幻'],
      },
    })

    expect(uri).not.toContain('tags%5B%5D=')
    expect(decodeURIComponent(uri)).toContain('tags=动漫')
    expect(decodeURIComponent(uri)).toContain('tags=科幻')
  })
})
