import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { RequireAuth } from './RequireAuth'
import { AuthProvider } from '../contexts/AuthProvider'
import { clearToken, setToken } from '../lib/auth-token'

function renderProtected() {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/secret']}>
        <Routes>
          <Route
            path="/secret"
            element={
              <RequireAuth>
                <div>conteudo-protegido</div>
              </RequireAuth>
            }
          />
          <Route path="/login" element={<div>tela-de-login</div>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('RequireAuth', () => {
  beforeEach(() => clearToken())
  afterEach(() => clearToken())

  it('redireciona para /login quando nao autenticado', () => {
    renderProtected()
    expect(screen.getByText('tela-de-login')).toBeInTheDocument()
    expect(screen.queryByText('conteudo-protegido')).not.toBeInTheDocument()
  })

  it('renderiza o conteudo quando autenticado', () => {
    setToken('jwt-1')
    renderProtected()
    expect(screen.getByText('conteudo-protegido')).toBeInTheDocument()
  })
})
