/**
 * Tipos compartilhados que espelham os enums e o envelope de paginacao do backend.
 * Centralizados aqui para serem reusados pelos demais contratos em `src/types/`.
 */

/** Tipo de categoria: entrada (RECEITA) ou saida (DESPESA). */
export type TipoCategoria = 'RECEITA' | 'DESPESA'

/** Forma de pagamento de uma transacao. */
export type FormaPagamento =
  | 'CARTAO_CREDITO'
  | 'CARTAO_DEBITO'
  | 'PIX'
  | 'DINHEIRO'
  | 'BOLETO'
  | 'TRANSFERENCIA'

/** Origem (banco/emissor) de uma fatura importada. */
export type OrigemFatura = 'NUBANK' | 'ITAU' | 'OUTROS'

/** Estado de processamento de uma fatura. */
export type StatusFatura = 'PENDENTE_PARSE' | 'PROCESSADA' | 'ERRO'

/**
 * Envelope de paginacao do Spring Data (`Page<T>`) retornado pelos endpoints de listagem.
 */
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
