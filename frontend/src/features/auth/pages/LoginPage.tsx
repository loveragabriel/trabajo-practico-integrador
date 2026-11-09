import { useId, useState, type SubmitEvent } from 'react'
import { Link, useLocation, useNavigate, type Location } from 'react-router-dom'
import { paths } from '@/app/paths'
import { login } from '@/features/auth/api/auth'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { ApiRequestError } from '@/shared/api/client'
import { PageHeader } from '@/shared/components/PageHeader'

export function LoginPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: Location } | null)?.from?.pathname ?? paths.home

  const emailId = useId()
  const passwordId = useId()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const session = await login({ email, password })
      signIn(session)
      navigate(from, { replace: true })
    } catch (err) {
      // el mensaje del backend queda en inglés para logs/debug; acá siempre mostramos español
      setError(
        err instanceof ApiRequestError && err.status === 401
          ? 'Email o contraseña incorrectos.'
          : 'No pudimos iniciar sesión. Intentá nuevamente.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="mx-auto max-w-sm">
      <PageHeader title="Ingresar" />
      <p className="text-slate-600">
        ¿No tenés cuenta?{' '}
        <Link to={paths.register} className="text-emerald-700 underline">
          Registrate
        </Link>
      </p>

      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <div className="space-y-2">
          <label htmlFor={emailId} className="block text-sm font-medium text-slate-800">
            Email
          </label>
          <input
            id={emailId}
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            autoComplete="email"
            className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 focus:border-emerald-600 focus:outline-2 focus:outline-emerald-600 disabled:bg-slate-100"
          />
        </div>

        <div className="space-y-2">
          <label htmlFor={passwordId} className="block text-sm font-medium text-slate-800">
            Contraseña
          </label>
          <input
            id={passwordId}
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            autoComplete="current-password"
            className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 focus:border-emerald-600 focus:outline-2 focus:outline-emerald-600 disabled:bg-slate-100"
          />
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-emerald-600 px-3 py-2 font-medium text-white hover:bg-emerald-700 disabled:opacity-60"
        >
          {submitting ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
    </section>
  )
}
