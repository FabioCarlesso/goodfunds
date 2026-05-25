import { http } from './http'
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types/auth'

/**
 * Chamadas a API de autenticacao do backend.
 * Ambas retornam um `AuthResponse` com o JWT; a persistencia do token e
 * responsabilidade do `AuthContext`/`auth-token`, nao destas funcoes.
 */

/** `POST /auth/login` — autentica e retorna o JWT. */
export async function login(payload: LoginRequest): Promise<AuthResponse> {
  const { data } = await http.post<AuthResponse>('/auth/login', payload)
  return data
}

/** `POST /auth/register` — cria a conta (com seed de categorias) e retorna o JWT. */
export async function register(payload: RegisterRequest): Promise<AuthResponse> {
  const { data } = await http.post<AuthResponse>('/auth/register', payload)
  return data
}
