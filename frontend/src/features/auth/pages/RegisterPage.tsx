import { useId, useState, type SubmitEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { paths } from '@/app/paths'
import { register } from '@/features/auth/api/auth'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { ZoneSelect, useZones } from '@/features/map'
import { ApiRequestError } from '@/shared/api/client'
import { PageHeader } from '@/shared/components/PageHeader'
import { ROLE_LABELS, type Role } from '@/shared/types/user'

const inputClass =
  'w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 focus:border-emerald-600 focus:outline-2 focus:outline-emerald-600 disabled:bg-slate-100'
const labelClass = 'block text-sm font-medium text-slate-800'

export function RegisterPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()

  // sin sesión todavía: el catálogo de zonas pide autenticación, así que este selector
  // va a quedar vacío hasta que el equipo decida abrir GET /api/zones sin token
  const { zones } = useZones(null)

  const fullNameId = useId()
  const emailId = useId()
  const passwordId = useId()
  const phoneId = useId()
  const priceId = useId()
  const capacityId = useId()

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [phone, setPhone] = useState('')
  const [role, setRole] = useState<Role>('CLIENT')
  const [zoneId, setZoneId] = useState('')
  const [defaultPrice, setDefaultPrice] = useState('')
  const [defaultCapacity, setDefaultCapacity] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const session = await register({
        fullName,
        email,
        password,
        phone: phone || undefined,
        role,
        zoneId,
        // solo tienen sentido para un paseador (RN-04); el backend los ignora para un dueño
        defaultPrice: role === 'WALKER' && defaultPrice ? Number(defaultPrice) : undefined,
        defaultCapacity: role === 'WALKER' && defaultCapacity ? Number(defaultCapacity) : undefined,
      })
      signIn(session)
      navigate(paths.home, { replace: true })
    } catch (err) {
      // el mensaje del backend queda en inglés para logs/debug; acá siempre mostramos español
      setError(
        err instanceof ApiRequestError && err.status === 409
          ? 'Ese email ya está registrado.'
          : err instanceof ApiRequestError && err.status === 404
            ? 'El barrio seleccionado ya no existe. Elegí otro.'
            : 'No pudimos crear la cuenta. Revisá los datos e intentá nuevamente.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="mx-auto max-w-sm">
      <PageHeader title="Crear cuenta" description="Registrate como dueño de mascota o como paseador." />

      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <div className="space-y-2">
          <label htmlFor={fullNameId} className={labelClass}>
            Nombre completo
          </label>
          <input
            id={fullNameId}
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            required
            autoComplete="name"
            className={inputClass}
          />
        </div>

        <div className="space-y-2">
          <label htmlFor={emailId} className={labelClass}>
            Email
          </label>
          <input
            id={emailId}
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            autoComplete="email"
            className={inputClass}
          />
        </div>

        <div className="space-y-2">
          <label htmlFor={passwordId} className={labelClass}>
            Contraseña
          </label>
          <input
            id={passwordId}
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            minLength={8}
            autoComplete="new-password"
            className={inputClass}
          />
        </div>

        <div className="space-y-2">
          <label htmlFor={phoneId} className={labelClass}>
            Teléfono (opcional)
          </label>
          <input
            id={phoneId}
            type="tel"
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
            autoComplete="tel"
            className={inputClass}
          />
        </div>

        <fieldset className="space-y-2">
          <legend className={labelClass}>Quiero registrarme como</legend>
          <div className="flex gap-4">
            {(Object.keys(ROLE_LABELS) as Role[]).map((value) => (
              <label key={value} className="flex items-center gap-2 text-slate-800">
                <input
                  type="radio"
                  name="role"
                  value={value}
                  checked={role === value}
                  onChange={() => setRole(value)}
                />
                {ROLE_LABELS[value]}
              </label>
            ))}
          </div>
        </fieldset>

        <ZoneSelect zones={zones} value={zoneId} onChange={setZoneId} label="Barrio" required />

        {role === 'WALKER' && (
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <label htmlFor={priceId} className={labelClass}>
                Precio por mascota (opcional)
              </label>
              <input
                id={priceId}
                type="number"
                min={0}
                step="0.01"
                value={defaultPrice}
                onChange={(event) => setDefaultPrice(event.target.value)}
                className={inputClass}
              />
            </div>
            <div className="space-y-2">
              <label htmlFor={capacityId} className={labelClass}>
                Cupo por paseo (opcional)
              </label>
              <input
                id={capacityId}
                type="number"
                min={1}
                step="1"
                value={defaultCapacity}
                onChange={(event) => setDefaultCapacity(event.target.value)}
                className={inputClass}
              />
            </div>
          </div>
        )}

        {error && <p className="text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-emerald-600 px-3 py-2 font-medium text-white hover:bg-emerald-700 disabled:opacity-60"
        >
          {submitting ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>
      </form>
    </section>
  )
}
