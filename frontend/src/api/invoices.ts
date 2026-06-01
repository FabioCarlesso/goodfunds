import { http } from './http'
import type { OrigemFatura } from '../types/common'
import type { Invoice, InvoiceDetail } from '../types/invoice'

/** `GET /invoices` — lista as faturas importadas do usuario. */
export async function listInvoices(): Promise<Invoice[]> {
  const { data } = await http.get<Invoice[]>('/invoices')
  return data
}

/** `GET /invoices/{id}` — fatura com as transacoes geradas. */
export async function getInvoice(id: string): Promise<InvoiceDetail> {
  const { data } = await http.get<InvoiceDetail>(`/invoices/${id}`)
  return data
}

/**
 * `POST /invoices/{id}/process` — processa uma fatura pendente: extrai os lancamentos
 * do PDF e gera as transacoes. Retorna a fatura atualizada (status `PROCESSADA`/`ERRO`).
 */
export async function processInvoice(id: string): Promise<Invoice> {
  const { data } = await http.post<Invoice>(`/invoices/${id}/process`)
  return data
}

/** `DELETE /invoices/{id}` — exclui a fatura, o PDF e as transacoes geradas. */
export async function deleteInvoice(id: string): Promise<void> {
  await http.delete(`/invoices/${id}`)
}

/**
 * `POST /invoices/upload` — envia um PDF de fatura (multipart). O backend deduz a
 * origem quando `origem` nao e informada. Sobrescreve o `Content-Type` do cliente
 * para `multipart/form-data` (o axios injeta o boundary).
 */
export async function uploadInvoice(file: File, origem?: OrigemFatura): Promise<Invoice> {
  const form = new FormData()
  form.append('file', file)
  if (origem) {
    form.append('origem', origem)
  }
  const { data } = await http.post<Invoice>('/invoices/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
