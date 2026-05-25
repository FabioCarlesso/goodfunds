import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ReportsPage from './ReportsPage'
import * as reportsApi from '../api/reports'

vi.mock('../api/reports')

describe('ReportsPage', () => {
  beforeEach(() => vi.clearAllMocks())

  it('carrega evolucao e gastos por categoria', async () => {
    vi.mocked(reportsApi.getEvolution).mockResolvedValue([
      { ref: '2026-04', receitas: 3000, despesas: 1000 },
      { ref: '2026-05', receitas: 3200, despesas: 1500 },
    ])
    vi.mocked(reportsApi.getByCategory).mockResolvedValue([
      { categoryId: 'cat-1', nome: 'Alimentacao', tipo: 'DESPESA', total: 500 },
    ])

    render(<ReportsPage />)

    expect(
      await screen.findByText('Evolucao mensal (receitas vs despesas)'),
    ).toBeInTheDocument()
    expect(reportsApi.getEvolution).toHaveBeenCalledTimes(1)
    expect(reportsApi.getByCategory).toHaveBeenCalledTimes(1)
  })

  it('exibe mensagem de erro quando o carregamento falha', async () => {
    vi.mocked(reportsApi.getEvolution).mockRejectedValue(new Error('boom'))
    vi.mocked(reportsApi.getByCategory).mockResolvedValue([])

    render(<ReportsPage />)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Nao foi possivel carregar os relatorios.',
    )
  })
})
