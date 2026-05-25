import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import InvoiceDetailPage from './InvoiceDetailPage'
import * as invoicesApi from '../api/invoices'
import type { InvoiceDetail } from '../types/invoice'

vi.mock('../api/invoices')

const detail: InvoiceDetail = {
  id: 'inv-1',
  arquivo: 'fatura-maio.pdf',
  origem: 'NUBANK',
  status: 'PROCESSADA',
  mesReferencia: '2026-05',
  totalValor: 150,
  createdAt: '2026-05-10T12:00:00Z',
  transactions: [
    {
      id: 'tx-1',
      descricao: 'Mercado',
      valor: 100,
      data: '2026-05-03',
      formaPagamento: 'CARTAO_CREDITO',
      categoryId: 'cat-1',
      categoryNome: 'Alimentacao',
      categoryTipo: 'DESPESA',
      invoiceId: 'inv-1',
      createdAt: '2026-05-10T12:00:00Z',
      updatedAt: '2026-05-10T12:00:00Z',
    },
  ],
}

function renderAt(id: string) {
  return render(
    <MemoryRouter initialEntries={[`/faturas/${id}`]}>
      <Routes>
        <Route path="/faturas/:id" element={<InvoiceDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('InvoiceDetailPage', () => {
  beforeEach(() => vi.clearAllMocks())

  it('exibe os metadados e as transacoes geradas', async () => {
    vi.mocked(invoicesApi.getInvoice).mockResolvedValue(detail)

    renderAt('inv-1')

    expect(await screen.findByText('Mercado')).toBeInTheDocument()
    expect(screen.getByText('Alimentacao')).toBeInTheDocument()
    expect(screen.getByText('Transacoes geradas (1)')).toBeInTheDocument()
    expect(invoicesApi.getInvoice).toHaveBeenCalledWith('inv-1')
  })
})
