import type { InputHTMLAttributes } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
}

/**
 * Campo de formulario com label associada via `htmlFor`/`id`.
 * Encapsula o estilo Tailwind compartilhado pelas telas de auth.
 */
export function Input({ label, id, className, ...props }: InputProps) {
  return (
    <div className="flex flex-col gap-1 text-left">
      <label htmlFor={id} className="text-sm font-medium text-slate-700">
        {label}
      </label>
      <input
        id={id}
        className={`rounded-md border border-slate-300 px-3 py-2 text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200 ${className ?? ''}`}
        {...props}
      />
    </div>
  )
}
