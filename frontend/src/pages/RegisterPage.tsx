import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register as registerRequest } from '../api/auth'
import { getApiErrorMessage } from '../api/errors'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'

/**
 * Tela de cadastro (`/register`). Cria a conta em `POST /auth/register` e, em caso
 * de sucesso, redireciona para `/login` com uma mensagem de confirmacao (o usuario
 * autentica em seguida). A senha exige no minimo 8 caracteres, alinhada ao backend.
 */
export default function RegisterPage() {
  const navigate = useNavigate()

  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await registerRequest({ nome, email, senha })
      navigate('/login', { replace: true, state: { registered: true } })
    } catch (err) {
      setError(getApiErrorMessage(err, 'Nao foi possivel criar a conta. Tente novamente.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="text-center text-3xl font-bold text-slate-900">Goodfunds</h1>
        <p className="mt-1 text-center text-sm text-slate-500">Crie sua conta</p>

        <form className="mt-6 flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
          <Input
            id="nome"
            label="Nome"
            type="text"
            autoComplete="name"
            required
            maxLength={255}
            value={nome}
            onChange={(e) => setNome(e.target.value)}
          />
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
            autoComplete="new-password"
            required
            minLength={8}
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
          />

          {error && (
            <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </p>
          )}

          <Button type="submit" disabled={submitting}>
            {submitting ? 'Criando conta...' : 'Cadastrar'}
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          Ja tem conta?{' '}
          <Link to="/login" className="font-medium text-slate-900 underline">
            Entrar
          </Link>
        </p>
      </div>
    </main>
  )
}
