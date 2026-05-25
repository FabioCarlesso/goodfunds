import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import LoginPage from './LoginPage'
import { AuthProvider } from '../contexts/AuthProvider'
import { clearToken, getToken } from '../lib/auth-token'
import * as authApi from '../api/auth'

vi.mock('../api/auth')

function renderLogin() {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<div>dashboard</div>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    clearToken()
    vi.clearAllMocks()
  })
  afterEach(() => clearToken())

  it('autentica, persiste o token e redireciona para o dashboard', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      token: 'jwt-xyz',
      tokenType: 'Bearer',
      expiresInMillis: 86400000,
    })
    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByLabelText('E-mail'), 'a@b.com')
    await user.type(screen.getByLabelText('Senha'), 'segredo12')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => expect(screen.getByText('dashboard')).toBeInTheDocument())
    expect(authApi.login).toHaveBeenCalledWith({ email: 'a@b.com', senha: 'segredo12' })
    expect(getToken()).toBe('jwt-xyz')
  })

  it('exibe mensagem de erro quando o login falha', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new Error('falha'))
    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByLabelText('E-mail'), 'a@b.com')
    await user.type(screen.getByLabelText('Senha'), 'errada')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Nao foi possivel entrar. Tente novamente.',
    )
    expect(getToken()).toBeNull()
    expect(screen.queryByText('dashboard')).not.toBeInTheDocument()
  })
})
