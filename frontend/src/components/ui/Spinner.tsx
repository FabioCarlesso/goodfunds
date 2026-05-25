/** Indicador de carregamento com `role="status"` para feedback de acoes assincronas. */
export function Spinner({ label = 'Carregando...' }: { label?: string }) {
  return (
    <div role="status" className="flex items-center gap-2 text-sm text-slate-500">
      <span
        aria-hidden="true"
        className="h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-slate-600"
      />
      <span>{label}</span>
    </div>
  )
}
