import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import App from './App'
import { clearToken, setToken } from './lib/auth-token'

describe('App', () => {
  beforeEach(() => {
    clearToken()
    window.history.pushState({}, '', '/')
  })
  afterEach(() => clearToken())

  it('redireciona rota protegida para o login quando nao autenticado', () => {
    render(<App />)
    expect(screen.getByRole('heading', { name: 'Goodfunds' })).toBeInTheDocument()
    expect(screen.getByText('Entre na sua conta')).toBeInTheDocument()
  })

  it('renderiza a home protegida quando autenticado', () => {
    setToken('jwt-1')
    render(<App />)
    expect(screen.getByText('Voce esta autenticado. Dashboard em construcao.')).toBeInTheDocument()
  })
})
