import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { login as loginRequest } from '../api/auth'
import { getApiErrorMessage } from '../api/errors'
import { useAuth } from '../hooks/useAuth'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'

/**
 * Tela de login (`/login`). Autentica em `POST /auth/login`, persiste o JWT via
 * `AuthContext` e redireciona para a rota de origem (ou Dashboard). Ja autenticado,
 * o roteamento em `App` impede o acesso a esta tela.
 */
export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const fromState = location.state as { from?: { pathname?: string } } | null
  const successMessage = (location.state as { registered?: boolean } | null)?.registered
    ? 'Conta criada com sucesso. Faca login para continuar.'
    : null
  const redirectTo = fromState?.from?.pathname ?? '/'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const { token } = await loginRequest({ email, senha })
      login(token)
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(getApiErrorMessage(err, 'Nao foi possivel entrar. Tente novamente.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="text-center text-3xl font-bold text-slate-900">Goodfunds</h1>
        <p className="mt-1 text-center text-sm text-slate-500">Entre na sua conta</p>

        {successMessage && (
          <p
            role="status"
            className="mt-4 rounded-md bg-emerald-50 px-3 py-2 text-sm text-emerald-700"
          >
            {successMessage}
          </p>
        )}

        <form className="mt-6 flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
          <Input
            id="email"
            label="E-mail"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <Input
            id="senha"
            label="Senha"
            type="password"
            autoComplete="current-password"
            required
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
          />

          {error && (
            <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </p>
          )}

          <Button type="submit" disabled={submitting}>
            {submitting ? 'Entrando...' : 'Entrar'}
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          Nao tem conta?{' '}
          <Link to="/register" className="font-medium text-slate-900 underline">
            Cadastre-se
          </Link>
        </p>
      </div>
    </main>
  )
}
