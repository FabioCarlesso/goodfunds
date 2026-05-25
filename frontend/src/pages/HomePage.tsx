import { useNavigate } from 'react-router-dom'
import { env } from '../lib/env'
import { useAuth } from '../hooks/useAuth'
import { Button } from '../components/ui/Button'

/**
 * Pagina inicial provisoria (rota protegida `/`). Confirma o login bem-sucedido e
 * oferece logout. Sera substituida pelo Dashboard real nas proximas atividades da
 * Sprint 4.
 */
export default function HomePage() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-slate-50 px-4 text-center">
      <h1 className="text-4xl font-bold text-slate-900">Goodfunds</h1>
      <p className="text-slate-600">Voce esta autenticado. Dashboard em construcao.</p>
      <code className="rounded bg-slate-200 px-3 py-1 text-sm text-slate-700">
        API: {env.apiBaseUrl}
      </code>
      <Button type="button" onClick={handleLogout}>
        Sair
      </Button>
    </main>
  )
}
