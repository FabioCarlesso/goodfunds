import type { OrigemFatura, StatusFatura } from './common'
import type { Transaction } from './transaction'

/**
 * Fatura importada (`/invoices`). `mesReferencia` e `totalValor` ficam nulos
 * enquanto a fatura nao foi processada (status `PENDENTE_PARSE`/`ERRO`).
 */
export interface Invoice {
  id: string
  arquivo: string
  origem: OrigemFatura
  status: StatusFatura
  mesReferencia: string | null
  totalValor: number | null
  createdAt: string
}

/** Detalhe da fatura (`GET /invoices/{id}`): a fatura mais as transacoes geradas. */
export interface InvoiceDetail extends Invoice {
  transactions: Transaction[]
}
