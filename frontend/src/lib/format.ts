/**
 * Utilitarios de formatacao e manipulacao do mes de referencia (`ref`, no formato
 * `YYYY-MM`) usados pelas telas. Centralizados para manter consistencia de moeda,
 * locale e aritmetica de meses.
 */

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

/** Formata um valor monetario em Reais (BRL). Trata `null`/`undefined` como zero. */
export function formatCurrency(value: number | null | undefined): string {
  return currencyFormatter.format(value ?? 0)
}

/** Mes corrente no formato `YYYY-MM` usado como parametro `ref` da API. */
export function currentRef(): string {
  return toRef(new Date())
}

/** Converte uma `Date` para o `ref` `YYYY-MM` (mes 1-based, com zero a esquerda). */
export function toRef(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

/** Soma (ou subtrai, com `delta` negativo) meses a um `ref`, retornando outro `ref`. */
export function addMonths(ref: string, delta: number): string {
  const [year, month] = ref.split('-').map(Number)
  return toRef(new Date(year, month - 1 + delta, 1))
}

/** Decompoe um `ref` `YYYY-MM` em `{ mes, ano }` (mes 1-based), como espera o backend. */
export function refToParts(ref: string): { mes: number; ano: number } {
  const [ano, mes] = ref.split('-').map(Number)
  return { mes, ano }
}

/** Formata um `ref` `YYYY-MM` como rotulo legivel, ex.: `maio de 2026`. */
export function formatMonthLabel(ref: string): string {
  const [year, month] = ref.split('-').map(Number)
  return new Date(year, month - 1, 1).toLocaleDateString('pt-BR', {
    month: 'long',
    year: 'numeric',
  })
}

/** Formata uma data ISO `YYYY-MM-DD` no padrao brasileiro `DD/MM/YYYY`. */
export function formatDate(iso: string): string {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString('pt-BR')
}
