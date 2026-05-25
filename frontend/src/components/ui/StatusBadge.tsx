import type { StatusFatura } from '../../types/common'

const STATUS_STYLES: Record<StatusFatura, string> = {
  PENDENTE_PARSE: 'bg-amber-100 text-amber-800',
  PROCESSADA: 'bg-emerald-100 text-emerald-800',
  ERRO: 'bg-red-100 text-red-800',
}

const STATUS_LABELS: Record<StatusFatura, string> = {
  PENDENTE_PARSE: 'Pendente',
  PROCESSADA: 'Processada',
  ERRO: 'Erro',
}

/** Selo colorido para o status de processamento de uma fatura. */
export function StatusBadge({ status }: { status: StatusFatura }) {
  return (
    <span
      className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[status]}`}
    >
      {STATUS_LABELS[status]}
    </span>
  )
}
