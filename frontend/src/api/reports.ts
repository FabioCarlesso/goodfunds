import { http } from './http'
import type {
  ByCategoryItem,
  EstimateResponse,
  MonthlyEntry,
  SummaryResponse,
} from '../types/report'

/** `GET /reports/summary` — visao geral do mes (omitir `ref` usa o mes corrente). */
export async function getSummary(ref?: string): Promise<SummaryResponse> {
  const { data } = await http.get<SummaryResponse>('/reports/summary', {
    params: ref ? { ref } : undefined,
  })
  return data
}

/** `GET /reports/by-category?ref=YYYY-MM` — total por categoria no mes. */
export async function getByCategory(ref: string): Promise<ByCategoryItem[]> {
  const { data } = await http.get<ByCategoryItem[]>('/reports/by-category', {
    params: { ref },
  })
  return data
}

/** `GET /reports/evolution?from&to` — receitas/despesas por mes no intervalo. */
export async function getEvolution(from: string, to: string): Promise<MonthlyEntry[]> {
  const { data } = await http.get<MonthlyEntry[]>('/reports/evolution', {
    params: { from, to },
  })
  return data
}

/** `GET /reports/estimate` — projecao do mes corrente com base no historico. */
export async function getEstimate(): Promise<EstimateResponse> {
  const { data } = await http.get<EstimateResponse>('/reports/estimate')
  return data
}
