import cryptoModule, { createHash, webcrypto } from 'node:crypto'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

type HashEncoding = 'hex' | 'base64' | 'base64url'
type HashCompat = (algorithm: string, data: string | NodeJS.ArrayBufferView, outputEncoding?: HashEncoding) => string

const cryptoModuleCompat = cryptoModule as typeof cryptoModule & {
  hash?: HashCompat
}

const cryptoCompat = (((globalThis as typeof globalThis & { crypto?: typeof webcrypto }).crypto) ?? webcrypto) as typeof webcrypto & {
  hash?: HashCompat
}

if (!globalThis.crypto) {
  ;(globalThis as typeof globalThis & { crypto?: typeof webcrypto }).crypto = cryptoCompat
}

const hashCompat: HashCompat = (algorithm, data, outputEncoding = 'hex') =>
  createHash(algorithm).update(data).digest(outputEncoding)

if (typeof cryptoCompat.hash !== 'function') {
  cryptoCompat.hash = hashCompat
}

if (typeof cryptoModuleCompat.hash !== 'function') {
  Object.defineProperty(cryptoModule, 'hash', {
    value: hashCompat,
    configurable: true,
  })
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/tests/setup.ts'],
  },
})
