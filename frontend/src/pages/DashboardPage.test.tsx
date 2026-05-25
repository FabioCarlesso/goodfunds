import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DashboardPage from './DashboardPage'
import * as reportsApi from '../api/reports'

vi.mock('../api/reports')

describe('DashboardPage', () => {
  beforeEach(() => vi.clearAllMocks())

  it('exibe o resumo do mes apos carregar', async () => {
    vi.mocked(reportsApi.getSummary).mockResolvedValue({
      ref: '2026-05',
      receitas: 3000,
      despesas: 230,
      orcado: 500,
      saldo: 2770,
      percentualOrcadoUsado: 46,
    })

    render(<DashboardPage />)

    expect(await screen.findByText(/2\.770,00/)).toBeInTheDocument() // saldo
    expect(screen.getByText(/3\.000,00/)).toBeInTheDocument() // receitas
    expect(screen.getByText(/230,00/)).toBeInTheDocument() // despesas
    expect(screen.getByText('46%')).toBeInTheDocument()
    expect(reportsApi.getSummary).toHaveBeenCalledTimes(1)
  })

  it('exibe mensagem de erro quando o resumo falha', async () => {
    vi.mocked(reportsApi.getSummary).mockRejectedValue(new Error('boom'))

    render(<DashboardPage />)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Nao foi possivel carregar o resumo do mes.',
    )
  })
})
