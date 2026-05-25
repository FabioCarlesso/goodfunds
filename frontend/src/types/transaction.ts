import type { FormaPagamento, TipoCategoria } from './common'

/**
 * Transacao (`/transactions`). Valores monetarios chegam como `number` (BigDecimal
 * serializado como numero JSON) e `data` como string ISO `YYYY-MM-DD`.
 */
export interface Transaction {
  id: string
  descricao: string
  valor: number
  data: string
  formaPagamento: FormaPagamento
  categoryId: string
  categoryNome: string
  categoryTipo: TipoCategoria
  invoiceId: string | null
  createdAt: string
  updatedAt: string
}
