/**
 * Configuracao de ambiente do frontend, lida das variaveis `VITE_*` do Vite.
 * Centraliza valores derivados de `import.meta.env` para facilitar testes e evitar
 * espalhar `import.meta.env` pelo codigo.
 */
export const env = {
  /** URL base da API REST do backend. */
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
} as const
