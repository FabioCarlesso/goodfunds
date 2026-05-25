import { useMemo, type ReactNode } from 'react'
import { useAuthToken } from '../hooks/useAuthToken'
import { AuthContext } from './auth-context'

/**
 * Provedor do estado de autenticacao. Reusa `useAuthToken` (que mantem o JWT em
 * estado React sincronizado com o `localStorage`) e o disponibiliza via Context
 * para que qualquer tela reaja a login/logout.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const { token, isAuthenticated, login, logout } = useAuthToken()
  const value = useMemo(
    () => ({ token, isAuthenticated, login, logout }),
    [token, isAuthenticated, login, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
