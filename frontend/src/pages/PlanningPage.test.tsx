import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PlanningPage from './PlanningPage'
import * as budgetsApi from '../api/budgets'
import * as categoriesApi from '../api/categories'
import * as reportsApi from '../api/reports'
import type { Budget } from '../types/budget'

vi.mock('../api/budgets')
vi.mock('../api/categories')
vi.mock('../api/reports')

const budget: Budget = {
  id: 'b-1',
  limite: 200,
  categoryId: 'cat-1',
  categoryNome: 'Alimentacao',
  categoryTipo: 'DESPESA',
  mes: 5,
  ano: 2026,
  createdAt: '2026-05-01T00:00:00Z',
  updatedAt: '2026-05-01T00:00:00Z',
}

describe('PlanningPage', () => {
  beforeEach(() => vi.clearAllMocks())

  it('lista orcamentos com o progresso de gasto', async () => {
    vi.mocked(budgetsApi.listBudgets).mockResolvedValue([budget])
    vi.mocked(reportsApi.getByCategory).mockResolvedValue([
      { categoryId: 'cat-1', nome: 'Alimentacao', tipo: 'DESPESA', total: 100 },
    ])
    vi.mocked(categoriesApi.listCategories).mockResolvedValue([])

    render(<PlanningPage />)

    expect(await screen.findByText('Alimentacao')).toBeInTheDocument()
    expect(screen.getByText('50% utilizado')).toBeInTheDocument()
  })

  it('cria um novo orcamento', async () => {
    vi.mocked(budgetsApi.listBudgets).mockResolvedValue([])
    vi.mocked(reportsApi.getByCategory).mockResolvedValue([])
    vi.mocked(categoriesApi.listCategories).mockResolvedValue([
      { id: 'cat-1', nome: 'Alimentacao', tipo: 'DESPESA' },
    ])
    vi.mocked(budgetsApi.createBudget).mockResolvedValue(budget)
    const user = userEvent.setup()

    render(<PlanningPage />)

    await waitFor(() =>
      expect(screen.getByRole('option', { name: 'Alimentacao' })).toBeInTheDocument(),
    )
    await user.selectOptions(screen.getByLabelText('Categoria'), 'cat-1')
    await user.type(screen.getByLabelText('Limite (R$)'), '300')
    await user.click(screen.getByRole('button', { name: 'Adicionar' }))

    await waitFor(() =>
      expect(budgetsApi.createBudget).toHaveBeenCalledWith(
        expect.objectContaining({ categoryId: 'cat-1', limite: 300 }),
      ),
    )
  })
})
