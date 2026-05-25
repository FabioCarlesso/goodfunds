import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

/**
 * Guarda de rota: renderiza o conteudo apenas para usuarios autenticados.
 * Sem JWT, redireciona para `/login` guardando a rota de origem em `state.from`
 * para um eventual retorno apos o login.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <>{children}</>
}
