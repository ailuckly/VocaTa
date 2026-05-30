import { createHash } from 'node:crypto'
import { config } from '@vue/test-utils'

type HashEncoding = 'hex' | 'base64' | 'base64url'

if (typeof (globalThis.crypto as typeof globalThis.crypto & { hash?: unknown })?.hash !== 'function') {
  ;(globalThis.crypto as typeof globalThis.crypto & {
    hash?: (algorithm: string, data: string | NodeJS.ArrayBufferView, outputEncoding?: HashEncoding) => string
  }).hash = (algorithm, data, outputEncoding = 'hex') =>
    createHash(algorithm).update(data).digest(outputEncoding)
}

config.global.stubs = {
  transition: false,
  'transition-group': false,
}
