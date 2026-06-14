import { createHash } from 'node:crypto'
import { config } from '@vue/test-utils'
import { vi } from 'vitest'

type HashEncoding = 'hex' | 'base64' | 'base64url'

if (typeof (globalThis.crypto as typeof globalThis.crypto & { hash?: unknown })?.hash !== 'function') {
  ;(globalThis.crypto as typeof globalThis.crypto & {
    hash?: (algorithm: string, data: string | NodeJS.ArrayBufferView, outputEncoding?: HashEncoding) => string
  }).hash = (algorithm, data, outputEncoding = 'hex') =>
    createHash(algorithm).update(data).digest(outputEncoding)
}

vi.spyOn(console, 'debug').mockImplementation(() => {})
vi.spyOn(console, 'info').mockImplementation(() => {})
vi.spyOn(console, 'log').mockImplementation(() => {})

config.global.stubs = {
  transition: false,
  'transition-group': false,
  RouterLink: {
    template: '<a><slot /></a>',
  },
  'el-form-item': {
    template: '<div><slot /></div>',
  },
  'el-icon': {
    template: '<span><slot /></span>',
  },
  ArrowRight: true,
  Expand: true,
  Microphone: true,
  Promotion: true,
  VideoPause: true,
}

config.global.directives = {
  loading: {},
}
