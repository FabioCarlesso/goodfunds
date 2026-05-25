import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { clearToken, setToken } from './lib/auth-token'
import * as reportsApi from './api/reports'

vi.mock('./api/reports')

describe('App', () => {
  beforeEach(() => {
    clearToken()
    vi.clearAllMocks()
    window.history.pushState({}, '', '/')
  })
  afterEach(() => clearToken())

  it('redireciona rota protegida para o login quando nao autenticado', () => {
    render(<App />)
    expect(screen.getByRole('heading', { name: 'Goodfunds' })).toBeInTheDocument()
    expect(screen.getByText('Entre na sua conta')).toBeInTheDocument()
  })

  it('renderiza o layout autenticado com o menu lateral em "/"', async () => {
    vi.mocked(reportsApi.getSummary).mockResolvedValue({
      ref: '2026-05',
      receitas: 3000,
      despesas: 230,
      orcado: 500,
      saldo: 2770,
      percentualOrcadoUsado: 46,
    })
    setToken('jwt-1')
    render(<App />)

    // "/" redireciona para o Dashboard dentro do AppLayout (menu lateral).
    expect(await screen.findByRole('link', { name: 'Planejamento' })).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument(),
    )
  })
})
