import { http } from './http'
import type { Budget, BudgetRequest } from '../types/budget'

/** `GET /budgets?ref=YYYY-MM` — orcamentos do mes de referencia. */
export async function listBudgets(ref: string): Promise<Budget[]> {
  const { data } = await http.get<Budget[]>('/budgets', { params: { ref } })
  return data
}

/** `POST /budgets` — cria um orcamento por categoria/mes. */
export async function createBudget(payload: BudgetRequest): Promise<Budget> {
  const { data } = await http.post<Budget>('/budgets', payload)
  return data
}

/** `PUT /budgets/{id}` — atualiza o limite de um orcamento. */
export async function updateBudget(id: string, payload: BudgetRequest): Promise<Budget> {
  const { data } = await http.put<Budget>(`/budgets/${id}`, payload)
  return data
}
