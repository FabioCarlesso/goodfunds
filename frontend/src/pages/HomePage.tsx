import { env } from '../lib/env'

/**
 * Pagina inicial provisoria do scaffold. Confirma que React + Tailwind estao
 * funcionando e exibe a URL da API configurada. Sera substituida pelas telas
 * reais (Login, Dashboard, etc.) nas proximas atividades da Sprint 4.
 */
export default function HomePage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-slate-50 px-4 text-center">
      <h1 className="text-4xl font-bold text-slate-900">Goodfunds</h1>
      <p className="text-slate-600">Scaffold do frontend pronto: Vite + React + TypeScript + Tailwind.</p>
      <code className="rounded bg-slate-200 px-3 py-1 text-sm text-slate-700">
        API: {env.apiBaseUrl}
      </code>
    </main>
  )
}
