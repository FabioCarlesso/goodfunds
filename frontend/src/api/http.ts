import axios from 'axios'
import { env } from '../lib/env'
import { clearToken, getToken } from '../lib/auth-token'

/**
 * Cliente HTTP unico do frontend, apontando para a API do backend.
 *
 * - Request interceptor: injeta `Authorization: Bearer <token>` quando ha JWT salvo.
 * - Response interceptor: em respostas 401, limpa o token e redireciona para `/login`.
 */
export const http = axios.create({
  baseURL: env.apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearToken()
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)
