import type { TipoCategoria } from './common'

/** Resposta de `GET /reports/summary`: visao geral do mes. */
export interface SummaryResponse {
  ref: string
  receitas: number
  despesas: number
  orcado: number
  saldo: number
  percentualOrcadoUsado: number
}

/** Item de `GET /reports/by-category`: total por categoria no mes. */
export interface ByCategoryItem {
  categoryId: string
  nome: string
  tipo: TipoCategoria
  total: number
}

/** Entrada de `GET /reports/evolution`: receitas e despesas de um mes. */
export interface MonthlyEntry {
  ref: string
  receitas: number
  despesas: number
}

/** Estimativa de uma categoria em `GET /reports/estimate`. */
export interface CategoryEstimate {
  categoryId: string
  categoryNome: string
  categoryTipo: TipoCategoria
  media: number
  realizado: number
  projecao: number
}

/** Totais consolidados da estimativa do mes corrente. */
export interface EstimateTotals {
  media: number
  realizado: number
  projecao: number
}

/** Resposta de `GET /reports/estimate`: projecao do mes corrente. */
export interface EstimateResponse {
  ref: string
  diasNoMes: number
  diasDecorridos: number
  consolidado: EstimateTotals
  categorias: CategoryEstimate[]
}
