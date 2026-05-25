import { createContext } from 'react'

/**
 * Estado global de autenticacao exposto pelo `AuthProvider`.
 * O `token` e o JWT corrente (ou `null` se deslogado); `login`/`logout`
 * persistem e limpam o token via `src/lib/auth-token`.
 */
export interface AuthContextValue {
  token: string | null
  isAuthenticated: boolean
  login: (token: string) => void
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
