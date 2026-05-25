import { useCallback, useState } from 'react'
import { clearToken, getToken, setToken } from '../lib/auth-token'

/**
 * Hook utilitario para ler e atualizar o JWT persistido no cliente.
 * Mantem o token em estado React para que componentes reajam a login/logout.
 */
export function useAuthToken() {
  const [token, setTokenState] = useState<string | null>(() => getToken())

  const login = useCallback((newToken: string) => {
    setToken(newToken)
    setTokenState(newToken)
  }, [])

  const logout = useCallback(() => {
    clearToken()
    setTokenState(null)
  }, [])

  return { token, isAuthenticated: token !== null, login, logout }
}
