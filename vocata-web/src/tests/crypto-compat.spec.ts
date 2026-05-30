import { describe, expect, it } from 'vitest'

describe('test runtime crypto compatibility', () => {
  it('provides crypto.hash for vite vue transforms', () => {
    expect(typeof (globalThis.crypto as typeof globalThis.crypto & { hash?: unknown })?.hash).toBe('function')
  })
})
