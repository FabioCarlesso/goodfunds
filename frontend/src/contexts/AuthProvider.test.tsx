import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { AuthProvider } from './AuthProvider'
import { useAuth } from '../hooks/useAuth'
import { clearToken, getToken, setToken } from '../lib/auth-token'

function Consumer() {
  const { isAuthenticated, token, login, logout } = useAuth()
  return (
    <div>
      <span data-testid="state">{isAuthenticated ? `auth:${token}` : 'anon'}</span>
      <button type="button" onClick={() => login('tkn-1')}>
        entrar
      </button>
      <button type="button" onClick={logout}>
        sair
      </button>
    </div>
  )
}

describe('AuthProvider', () => {
  beforeEach(() => clearToken())
  afterEach(() => clearToken())

  it('inicia anonimo quando nao ha token persistido', () => {
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>,
    )
    expect(screen.getByTestId('state')).toHaveTextContent('anon')
  })

  it('hidrata o estado a partir do token persistido', () => {
    setToken('persisted')
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>,
    )
    expect(screen.getByTestId('state')).toHaveTextContent('auth:persisted')
  })

  it('login persiste o token e logout o limpa', async () => {
    const user = userEvent.setup()
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>,
    )

    await user.click(screen.getByRole('button', { name: 'entrar' }))
    expect(screen.getByTestId('state')).toHaveTextContent('auth:tkn-1')
    expect(getToken()).toBe('tkn-1')

    await user.click(screen.getByRole('button', { name: 'sair' }))
    expect(screen.getByTestId('state')).toHaveTextContent('anon')
    expect(getToken()).toBeNull()
  })
})

describe('useAuth', () => {
  it('lanca quando usado fora do AuthProvider', () => {
    function Orphan() {
      useAuth()
      return null
    }
    expect(() => render(<Orphan />)).toThrow('useAuth deve ser usado dentro de um AuthProvider')
  })
})
