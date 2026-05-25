/** Mensagem de erro padronizada (`role="alert"`) para falhas de carregamento. */
export function ErrorState({ message }: { message: string }) {
  return (
    <p role="alert" className="rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">
      {message}
    </p>
  )
}

/** Estado vazio padronizado, exibido quando uma listagem nao tem itens. */
export function EmptyState({ message }: { message: string }) {
  return (
    <p className="rounded-md border border-dashed border-slate-300 bg-white px-4 py-8 text-center text-sm text-slate-500">
      {message}
    </p>
  )
}
