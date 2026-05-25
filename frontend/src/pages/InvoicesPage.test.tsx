import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import InvoicesPage from './InvoicesPage'
import * as invoicesApi from '../api/invoices'
import type { Invoice } from '../types/invoice'

vi.mock('../api/invoices')

const sampleInvoice: Invoice = {
  id: 'inv-1',
  arquivo: 'fatura-maio.pdf',
  origem: 'NUBANK',
  status: 'PROCESSADA',
  mesReferencia: '2026-05',
  totalValor: 1234.56,
  createdAt: '2026-05-10T12:00:00Z',
}

function renderPage() {
  return render(
    <MemoryRouter>
      <InvoicesPage />
    </MemoryRouter>,
  )
}

describe('InvoicesPage', () => {
  beforeEach(() => vi.clearAllMocks())

  it('lista as faturas importadas', async () => {
    vi.mocked(invoicesApi.listInvoices).mockResolvedValue([sampleInvoice])

    renderPage()

    expect(await screen.findByRole('link', { name: 'fatura-maio.pdf' })).toBeInTheDocument()
    expect(screen.getByText('Processada')).toBeInTheDocument()
    expect(screen.getByText(/1\.234,56/)).toBeInTheDocument()
  })

  it('mostra estado vazio quando nao ha faturas', async () => {
    vi.mocked(invoicesApi.listInvoices).mockResolvedValue([])

    renderPage()

    expect(await screen.findByText('Nenhuma fatura importada ainda.')).toBeInTheDocument()
  })

  it('faz upload de uma fatura e recarrega a lista', async () => {
    vi.mocked(invoicesApi.listInvoices).mockResolvedValue([])
    vi.mocked(invoicesApi.uploadInvoice).mockResolvedValue(sampleInvoice)
    const user = userEvent.setup()

    renderPage()
    await screen.findByText('Nenhuma fatura importada ainda.')

    const file = new File(['conteudo'], 'fatura.pdf', { type: 'application/pdf' })
    await user.upload(screen.getByLabelText('Arquivo PDF'), file)
    await user.click(screen.getByRole('button', { name: 'Enviar' }))

    await waitFor(() => expect(invoicesApi.uploadInvoice).toHaveBeenCalledWith(file, undefined))
    expect(invoicesApi.listInvoices).toHaveBeenCalledTimes(2) // carga inicial + recarga
  })
})
