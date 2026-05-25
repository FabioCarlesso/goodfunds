import type { ReactNode, SelectHTMLAttributes } from 'react'

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  children: ReactNode
}

/**
 * Campo de selecao com label associada via `htmlFor`/`id`, no mesmo estilo do `Input`.
 */
export function Select({ label, id, className, children, ...props }: SelectProps) {
  return (
    <div className="flex flex-col gap-1 text-left">
      <label htmlFor={id} className="text-sm font-medium text-slate-700">
        {label}
      </label>
      <select
        id={id}
        className={`rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200 ${className ?? ''}`}
        {...props}
      >
        {children}
      </select>
    </div>
  )
}
