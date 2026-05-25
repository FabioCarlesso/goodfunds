import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import RegisterPage from './RegisterPage'
import LoginPage from './LoginPage'
import { AuthProvider } from '../contexts/AuthProvider'
import { clearToken } from '../lib/auth-token'
import * as authApi from '../api/auth'

vi.mock('../api/auth')

function renderRegister() {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/register']}>
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('RegisterPage', () => {
  beforeEach(() => {
    clearToken()
    vi.clearAllMocks()
  })
  afterEach(() => clearToken())

  it('cria a conta e redireciona para /login com mensagem de sucesso', async () => {
    vi.mocked(authApi.register).mockResolvedValue({
      token: 'jwt-novo',
      tokenType: 'Bearer',
      expiresInMillis: 86400000,
    })
    const user = userEvent.setup()
    renderRegister()

    await user.type(screen.getByLabelText('Nome'), 'Fulano')
    await user.type(screen.getByLabelText('E-mail'), 'novo@b.com')
    await user.type(screen.getByLabelText('Senha'), 'segredo12')
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    await waitFor(() => expect(screen.getByRole('status')).toBeInTheDocument())
    expect(authApi.register).toHaveBeenCalledWith({
      nome: 'Fulano',
      email: 'novo@b.com',
      senha: 'segredo12',
    })
    expect(screen.getByRole('status')).toHaveTextContent(
      'Conta criada com sucesso. Faca login para continuar.',
    )
  })

  it('exibe mensagem de erro quando o cadastro falha', async () => {
    vi.mocked(authApi.register).mockRejectedValue(new Error('falha'))
    const user = userEvent.setup()
    renderRegister()

    await user.type(screen.getByLabelText('Nome'), 'Fulano')
    await user.type(screen.getByLabelText('E-mail'), 'novo@b.com')
    await user.type(screen.getByLabelText('Senha'), 'segredo12')
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Nao foi possivel criar a conta. Tente novamente.',
    )
  })
})
