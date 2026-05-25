import { describe, expect, it, vi } from 'vitest'
import { http } from './http'
import { login, register } from './auth'

describe('auth api', () => {
  it('login faz POST /auth/login com email e senha e retorna o corpo', async () => {
    const response = { token: 'jwt-1', tokenType: 'Bearer', expiresInMillis: 86400000 }
    const post = vi.spyOn(http, 'post').mockResolvedValue({ data: response })

    const result = await login({ email: 'a@b.com', senha: 'segredo12' })

    expect(post).toHaveBeenCalledWith('/auth/login', { email: 'a@b.com', senha: 'segredo12' })
    expect(result).toEqual(response)
    post.mockRestore()
  })

  it('register faz POST /auth/register com nome, email e senha e retorna o corpo', async () => {
    const response = { token: 'jwt-2', tokenType: 'Bearer', expiresInMillis: 86400000 }
    const post = vi.spyOn(http, 'post').mockResolvedValue({ data: response })

    const result = await register({ nome: 'Fulano', email: 'a@b.com', senha: 'segredo12' })

    expect(post).toHaveBeenCalledWith('/auth/register', {
      nome: 'Fulano',
      email: 'a@b.com',
      senha: 'segredo12',
    })
    expect(result).toEqual(response)
    post.mockRestore()
  })
})
