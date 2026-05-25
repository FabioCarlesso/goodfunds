/**
 * Contratos de autenticacao espelhando os DTOs do backend (`/auth/*`).
 * Mantidos em `src/types/` para que telas e clientes HTTP compartilhem a mesma tipagem.
 */

/** Corpo de `POST /auth/login`. */
export interface LoginRequest {
  email: string
  senha: string
}

/** Corpo de `POST /auth/register`. */
export interface RegisterRequest {
  nome: string
  email: string
  senha: string
}

/** Resposta de `/auth/login` e `/auth/register`. */
export interface AuthResponse {
  token: string
  tokenType: string
  expiresInMillis: number
}
