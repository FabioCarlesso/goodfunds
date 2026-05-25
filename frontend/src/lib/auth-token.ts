/**
 * Persistencia do JWT no cliente.
 *
 * A estrategia de armazenamento (localStorage no MVP) fica isolada aqui para que
 * o restante do codigo dependa apenas destas funcoes. Trocar por sessionStorage
 * ou cookie no futuro exige mudar somente este arquivo.
 */
const TOKEN_KEY = 'goodfunds.token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}
