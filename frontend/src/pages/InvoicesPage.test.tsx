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

const pendingInvoice: Invoice = {
  id: 'inv-2',
  arquivo: 'fatura-junho.pdf',
  origem: 'NUBANK',
  status: 'PENDENTE_PARSE',
  mesReferencia: null,
  totalValor: null,
  createdAt: '2026-06-01T12:00:00Z',
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

  it('processa uma fatura pendente e recarrega a lista', async () => {
    vi.mocked(invoicesApi.listInvoices).mockResolvedValue([pendingInvoice])
    vi.mocked(invoicesApi.processInvoice).mockResolvedValue({
      ...pendingInvoice,
      status: 'PROCESSADA',
    })
    const user = userEvent.setup()

    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Processar' }))

    await waitFor(() => expect(invoicesApi.processInvoice).toHaveBeenCalledWith('inv-2'))
    expect(invoicesApi.listInvoices).toHaveBeenCalledTimes(2) // carga inicial + recarga
  })

  it('exclui uma fatura apos confirmacao e recarrega a lista', async () => {
    vi.mocked(invoicesApi.listInvoices).mockResolvedValue([sampleInvoice])
    vi.mocked(invoicesApi.deleteInvoice).mockResolvedValue()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const user = userEvent.setup()

    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Excluir' }))

    await waitFor(() => expect(invoicesApi.deleteInvoice).toHaveBeenCalledWith('inv-1'))
    expect(invoicesApi.listInvoices).toHaveBeenCalledTimes(2) // carga inicial + recarga
  })

  it('nao exclui a fatura quando a confirmacao e cancelada', async () => {
    vi.mocked(invoicesApi.listInvoices).mockResolvedValue([sampleInvoice])
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const user = userEvent.setup()

    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Excluir' }))

    expect(invoicesApi.deleteInvoice).not.toHaveBeenCalled()
  })
})
