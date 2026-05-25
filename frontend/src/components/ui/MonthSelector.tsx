import { addMonths } from '../../lib/format'

interface MonthSelectorProps {
  /** Mes corrente no formato `YYYY-MM`. */
  value: string
  /** Callback com o novo `ref` ao navegar ou escolher um mes. */
  onChange: (ref: string) => void
  id?: string
  'aria-label'?: string
}

/**
 * Seletor de mes de referencia: botoes de mes anterior/proximo em volta de um
 * `<input type="month">`. Opera sobre `ref` no formato `YYYY-MM`.
 */
export function MonthSelector({
  value,
  onChange,
  id = 'mes',
  'aria-label': ariaLabel = 'Mes de referencia',
}: MonthSelectorProps) {
  return (
    <div className="flex items-center gap-1">
      <button
        type="button"
        aria-label="Mes anterior"
        onClick={() => onChange(addMonths(value, -1))}
        className="rounded-md border border-slate-300 px-2 py-1.5 text-slate-600 transition hover:bg-slate-100"
      >
        ‹
      </button>
      <input
        id={id}
        type="month"
        aria-label={ariaLabel}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="rounded-md border border-slate-300 px-3 py-1.5 text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
      />
      <button
        type="button"
        aria-label="Proximo mes"
        onClick={() => onChange(addMonths(value, 1))}
        className="rounded-md border border-slate-300 px-2 py-1.5 text-slate-600 transition hover:bg-slate-100"
      >
        ›
      </button>
    </div>
  )
}
