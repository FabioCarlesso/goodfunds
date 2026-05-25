import type { TipoCategoria } from './common'

/** Orcamento mensal por categoria (`GET /budgets`). */
export interface Budget {
  id: string
  limite: number
  categoryId: string
  categoryNome: string
  categoryTipo: TipoCategoria
  mes: number
  ano: number
  createdAt: string
  updatedAt: string
}

/** Corpo de criacao/edicao de orcamento (`POST`/`PUT /budgets`). */
export interface BudgetRequest {
  limite: number
  categoryId: string
  mes: number
  ano: number
}
