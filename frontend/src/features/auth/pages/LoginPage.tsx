import { useId, useState, type SubmitEvent } from 'react'
import { Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { login } from '@/features/auth/api/auth'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { ApiRequestError } from '@/shared/api/client'
import { AppScreen } from '@/shared/components/AppScreen'
import { Icon } from '@/shared/components/Icon'
import { TextField } from '@/shared/components/TextField'

export function LoginPage() {
  const { signIn } = useAuth()

  const emailId = useId()
  const passwordId = useId()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const session = await login({ email, password })
      signIn(session)
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
    <>
      {/* fuera de AppScreen a propósito: se apoya contra el borde real de la ventana,
          no contra el de la columna centrada (max-w-md) de acá abajo */}
      <div className="mt-4 ml-5 mb-4  flex w-fit items-center gap-1.5 rounded-full bg-surface-container-lowest px-3 py-1.5 shadow-sm">
        <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-on-primary">
          <Icon name="pets" filled className="text-[15px]" />
        </div>
        <span className="text-label-md text-primary">Paseadores</span>
      </div>

      <AppScreen>
        <section className="mt-4 mb-5">
          <div className="mb-2.5 inline-flex items-center gap-2 rounded-full bg-secondary-fixed/50 px-2.5 py-1 text-on-secondary-fixed">
            <Icon name="verified_user" filled className="text-[16px] text-secondary" />
            <span className="text-label-sm font-medium">Paseos grupales en CABA</span>
          </div>
          <h1 className="text-headline-lg font-bold tracking-tight text-on-surface">¡Hola de nuevo! 🐾</h1>
          <p className="mt-1.5 leading-relaxed text-body-md text-on-surface-variant">
            Ingresá a tu cuenta para publicar o reservar paseos grupales en tu barrio.
          </p>
        </section>

        <div className="space-y-4 rounded-xl bg-surface-container-lowest p-5 shadow-sm">
          <form onSubmit={handleSubmit} className="space-y-4">
            <fieldset disabled={submitting} className="space-y-4">
              <TextField
                id={emailId}
                label="Correo electrónico"
                icon="mail"
                type="email"
                autoComplete="email"
                placeholder="martina@ejemplo.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
              />

              <TextField
                id={passwordId}
                label="Contraseña"
                icon="lock"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="••••••••••••"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
                trailing={
                  <button
                    type="button"
                    aria-label="Mostrar u ocultar contraseña"
                    onClick={() => setShowPassword((value) => !value)}
                    className="absolute right-3.5 flex h-7 w-7 items-center justify-center text-on-surface-variant transition-transform hover:text-primary active:scale-90"
                  >
                    <Icon name={showPassword ? 'visibility_off' : 'visibility'} className="text-[20px]" />
                  </button>
                }
              />

              {error && <p role="alert" className="text-body-sm text-error">{error}</p>}

              <button
                type="submit"
                className="mt-2 flex h-[52px] w-full items-center justify-center gap-2 rounded-full bg-primary-container text-label-lg text-on-primary shadow-sm transition-all active:scale-[0.98] active:bg-primary disabled:opacity-60"
              >
                <span>{submitting ? 'Ingresando…' : 'Ingresar'}</span>
                <Icon name="arrow_forward" className="text-[18px]" />
              </button>
            </fieldset>
          </form>
        </div>

        <div className="mb-6 mt-4 rounded-xl bg-surface-container-low p-4 text-center">
          <p className="text-body-md text-on-surface">¿No tenés cuenta todavía?</p>
          <Link
            to={paths.register}
            className="mt-1 inline-flex items-center gap-1.5 text-label-lg text-primary underline-offset-4 hover:underline"
          >
            <span>Creá tu cuenta gratis</span>
            <Icon name="person_add" className="text-[18px]" />
          </Link>
        </div>

        <div className="flex items-center justify-center gap-2 pb-6 text-on-surface-variant/80">
          <Icon name="shield" filled className="text-[18px] text-secondary" />
          <span className="text-center text-caption">Encontrá paseos grupales en tu barrio</span>
        </div>
      </AppScreen>
    </>
  )
}
