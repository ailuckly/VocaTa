/**
 * 本地 SVG 头像生成器
 * 根据名字生成彩色字母头像，用于图片加载失败时的 fallback
 */

const PALETTE = [
  ['#818cf8', '#312e81'], // indigo
  ['#34d399', '#064e3b'], // emerald
  ['#f472b6', '#831843'], // pink
  ['#fb923c', '#7c2d12'], // orange
  ['#60a5fa', '#1e3a5f'], // blue
  ['#a78bfa', '#3b0764'], // violet
  ['#4ade80', '#14532d'], // green
  ['#f87171', '#7f1d1d'], // red
  ['#facc15', '#713f12'], // yellow
  ['#2dd4bf', '#134e4a'], // teal
]

function pickColor(name: string): [string, string] {
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return PALETTE[Math.abs(hash) % PALETTE.length]
}

function initials(name: string): string {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  return name.slice(0, 2).toUpperCase()
}

/**
 * 生成 SVG data URL 头像
 * @param name 用于生成颜色和字母的名字
 * @param size SVG 尺寸（默认 64）
 */
export function generateAvatarSvg(name: string, size = 64): string {
  const [bg, fg] = pickColor(name || '?')
  const text = initials(name || '?')
  const fontSize = size * 0.38

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <rect width="${size}" height="${size}" fill="${bg}" rx="${size * 0.2}"/>
  <text x="50%" y="50%" dominant-baseline="central" text-anchor="middle"
    font-family="system-ui,sans-serif" font-size="${fontSize}" font-weight="700" fill="${fg}">${text}</text>
</svg>`

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

/**
 * 处理 img 的 onerror，替换为本地生成的头像
 */
export function onAvatarError(event: Event, name: string) {
  const img = event.target as HTMLImageElement
  img.onerror = null // 防止循环触发
  img.src = generateAvatarSvg(name)
}
