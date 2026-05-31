import { describe, expect, it } from 'vitest'
import { generateAvatarSvg } from '@/utils/avatar'

function decodeAvatarSvg(dataUrl: string): string {
  const [, encodedSvg] = dataUrl.split(',')
  return decodeURIComponent(encodedSvg)
}

describe('generateAvatarSvg', () => {
  it('uses trimmed initials for a single display name', () => {
    const svg = decodeAvatarSvg(generateAvatarSvg('  ada  '))

    expect(svg).toContain('>AD<')
  })

  it('escapes initials before embedding them into SVG text', () => {
    const svg = decodeAvatarSvg(generateAvatarSvg('<img src=x onerror=alert(1)>'))

    expect(svg).toContain('&lt;')
    expect(svg).not.toContain('<img')
  })
})
