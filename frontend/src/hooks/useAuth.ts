import { useContext } from 'react'
import { AuthContext, type AuthContextValue } from '../contexts/auth-context'

/**
 * Acessa o estado de autenticacao provido pelo `AuthProvider`.
 * Lanca caso seja usado fora do provedor, evitando estado de auth silenciosamente nulo.
 */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (context === null) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider')
  }
  return context
}
