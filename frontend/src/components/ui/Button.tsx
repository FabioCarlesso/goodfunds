import type { ButtonHTMLAttributes } from 'react'

/**
 * Botao primario das telas de auth. Desabilita e baixa a opacidade durante
 * acoes assincronas (estado `disabled`).
 */
export function Button({ className, ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      className={`rounded-md bg-slate-900 px-4 py-2 font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-60 ${className ?? ''}`}
      {...props}
    />
  )
}
