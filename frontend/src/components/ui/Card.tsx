import type { ReactNode } from 'react'

/** Cartao base com borda, fundo branco e sombra suave, padrao das telas internas. */
export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div
      className={`rounded-xl border border-slate-200 bg-white p-5 shadow-sm ${className ?? ''}`}
    >
      {children}
    </div>
  )
}
