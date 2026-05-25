import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { http } from './http'
import { clearToken, setToken } from '../lib/auth-token'

/**
 * Exercita o request interceptor do cliente HTTP rodando a cadeia de
 * interceptors do axios diretamente sobre um config, sem fazer rede.
 */
async function runRequestInterceptors(config: Record<string, unknown>) {
  // O axios guarda os handlers registrados em `interceptors.request.handlers`.
  const handlers = http.interceptors.request as unknown as {
    handlers: Array<{ fulfilled: (c: unknown) => unknown } | null>
  }
  let current: unknown = config
  for (const handler of handlers.handlers) {
    if (handler) {
      current = await handler.fulfilled(current)
    }
  }
  return current as { headers: Record<string, string> }
}

describe('http client', () => {
  beforeEach(() => {
    clearToken()
  })

  afterEach(() => {
    clearToken()
  })

  it('injeta o header Authorization: Bearer quando ha token salvo', async () => {
    setToken('jwt-123')
    const result = await runRequestInterceptors({ headers: {} })
    expect(result.headers.Authorization).toBe('Bearer jwt-123')
  })

  it('nao injeta Authorization quando nao ha token', async () => {
    const result = await runRequestInterceptors({ headers: {} })
    expect(result.headers.Authorization).toBeUndefined()
  })
})
