import axios from 'axios'

/**
 * Forma do corpo de erro retornado pelo backend (RFC 7807 / `ProblemDetail`).
 * Erros de validacao (`@Valid`) incluem o mapa `errors` (campo -> mensagem).
 */
interface ProblemDetail {
  title?: string
  detail?: string
  status?: number
  errors?: Record<string, string>
}

/**
 * Extrai uma mensagem amigavel de um erro de chamada HTTP.
 *
 * Prioriza, nesta ordem: a primeira mensagem de validacao de campo, o `detail`
 * e o `title` do `ProblemDetail`; em falha de rede usa uma mensagem dedicada;
 * caso contrario retorna o `fallback` informado pela tela.
 */
export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const problem = error.response?.data as ProblemDetail | undefined
    if (problem) {
      const firstFieldError = problem.errors ? Object.values(problem.errors)[0] : undefined
      if (firstFieldError) return firstFieldError
      if (problem.detail) return problem.detail
      if (problem.title) return problem.title
    }
    if (!error.response) return 'Nao foi possivel conectar ao servidor. Tente novamente.'
  }
  return fallback
}
