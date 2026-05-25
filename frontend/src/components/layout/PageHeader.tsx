import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: string
  description?: string
  /** Acoes alinhadas a direita do titulo (ex.: seletor de mes). */
  actions?: ReactNode
}

/** Cabecalho padrao das telas: titulo, descricao opcional e area de acoes. */
export function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <header className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
        {description && <p className="mt-1 text-sm text-slate-500">{description}</p>}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </header>
  )
}
