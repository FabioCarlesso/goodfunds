import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { Button } from '../ui/Button'

/** Itens do menu lateral, na ordem em que aparecem. */
const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/faturas', label: 'Faturas' },
  { to: '/planejamento', label: 'Planejamento' },
  { to: '/relatorios', label: 'Relatorios' },
]

/**
 * Layout das telas autenticadas: menu lateral fixo de navegacao + area de conteudo
 * renderizada via `<Outlet />`. O botao "Sair" limpa o JWT e volta para o login.
 */
export function AppLayout() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex min-h-screen bg-slate-50">
      <aside className="flex w-56 shrink-0 flex-col border-r border-slate-200 bg-white">
        <div className="px-6 py-5 text-2xl font-bold text-slate-900">Goodfunds</div>
        <nav aria-label="Navegacao principal" className="flex flex-1 flex-col gap-1 px-3">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `rounded-md px-3 py-2 text-sm font-medium transition ${
                  isActive ? 'bg-slate-900 text-white' : 'text-slate-700 hover:bg-slate-100'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-200 p-3">
          <Button type="button" onClick={handleLogout} className="w-full">
            Sair
          </Button>
        </div>
      </aside>

      <main className="flex-1 overflow-x-hidden px-6 py-8">
        <div className="mx-auto max-w-5xl">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
