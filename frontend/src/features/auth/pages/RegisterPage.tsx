import { useId, useState, type SubmitEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { paths } from '@/app/paths'
import { register } from '@/features/auth/api/auth'
import { usePublicZones } from '@/features/auth/hooks/usePublicZones'
import { useAuth } from '@/features/auth/hooks/useAuth'
import type { Zone } from '@/features/map'
import { updateCoverage } from '@/features/profile/api/coverage'
import { ApiRequestError } from '@/shared/api/client'
import { writeSession, type Session } from '@/shared/api/session'
import { AppScreen } from '@/shared/components/AppScreen'
import { Icon } from '@/shared/components/Icon'
import { IconButton } from '@/shared/components/IconButton'
import { SelectField } from '@/shared/components/SelectField'
import { TextField } from '@/shared/components/TextField'
import type { Role } from '@/shared/types/user'

interface RoleCardProps {
  selected: boolean
  onClick: () => void
  icon: string
  iconFilled?: boolean
  title: string
  subtitle: string
}

// solo se usa acá (elegir CLIENT/WALKER al registrarse); no amerita vivir en shared
function RoleCard({ selected, onClick, icon, iconFilled, title, subtitle }: RoleCardProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={selected}
      className={`relative flex flex-col items-start rounded-xl p-3.5 text-left shadow-sm transition-all duration-200 ${
        selected ? 'bg-primary-fixed/30' : 'bg-surface-container-lowest'
      }`}
    >
      <span
        className={`absolute top-3 right-3 flex h-5 w-5 items-center justify-center rounded-full transition-opacity ${
          selected ? 'bg-primary text-on-primary opacity-100' : 'bg-surface-container text-outline opacity-0'
        }`}
      >
        <Icon name="check" className="text-[15px]" />
      </span>
      <span
        className={`mb-3 flex h-10 w-10 items-center justify-center rounded-full shadow-sm ${
          selected ? 'bg-surface-container-lowest text-primary' : 'bg-surface-container-low text-on-surface-variant'
        }`}
      >
        <Icon name={icon} filled={iconFilled} className="text-[24px]" />
      </span>
      <span className={`mb-1 text-title-sm font-semibold ${selected ? 'text-primary' : 'text-on-surface'}`}>{title}</span>
      <span className="text-caption text-on-surface-variant">{subtitle}</span>
    </button>
  )
}

// paso 2, solo para WALKER: configura PUT /api/walkers/me (RN-08, necesario antes de publicar).
// Es opcional en este momento del registro porque el mismo formulario ya existe en el perfil.
function WalkerCoverageStep({ zones, onDone }: { zones: Zone[]; onDone: () => void }) {
  const priceId = useId()
  const capacityId = useId()

  const [selectedZones, setSelectedZones] = useState<string[]>([])
  const [price, setPrice] = useState('')
  const [capacity, setCapacity] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  function toggleZone(zoneId: string) {
    setSelectedZones((current) => (current.includes(zoneId) ? current.filter((id) => id !== zoneId) : [...current, zoneId]))
  }

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving) return
    setSaving(true)
    setError(null)
    try {
      await updateCoverage({
        defaultPrice: price === '' ? null : Number(price),
        defaultCapacity: capacity === '' ? null : Number(capacity),
        zoneIds: selectedZones,
      })
      onDone()
    } catch (failure) {
      setError(
        failure instanceof ApiRequestError
          ? (failure.fieldErrors?.[0]?.message ?? failure.message)
          : 'No pudimos guardar tu cobertura. Podés configurarla después desde tu perfil.',
      )
    } finally {
      setSaving(false)
    }
  }

  return (
    <AppScreen>
      <div className="mt-4 mb-6">
        <div className="mb-1.5 flex items-center gap-2">
          <span className="text-headline-lg font-bold text-on-surface">Configurá tu perfil</span>
          <span className="text-[26px]">🐾</span>
        </div>
        <p className="text-body-md text-on-surface-variant">
          Elegí dónde ofrecés tus paseos y valores de referencia. Podés cambiarlo cuando quieras desde tu perfil.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <fieldset disabled={saving} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <TextField
              id={priceId}
              label="Precio por mascota"
              icon="payments"
              type="number"
              min={0}
              step="0.01"
              placeholder="2500"
              value={price}
              onChange={(event) => setPrice(event.target.value)}
            />
            <TextField
              id={capacityId}
              label="Cupo por paseo"
              icon="groups"
              type="number"
              min={1}
              step="1"
              placeholder="3"
              value={capacity}
              onChange={(event) => setCapacity(event.target.value)}
            />
          </div>

          <div className="space-y-2">
            <span className="block text-label-md font-semibold text-on-surface">
              Zonas donde ofrecés paseos · {selectedZones.length} seleccionadas
            </span>
            <div className="flex max-h-52 flex-wrap gap-2 overflow-y-auto rounded-xl bg-surface-container-low p-3">
              {zones.map((zone) => {
                const selected = selectedZones.includes(zone.id)
                return (
                  <button
                    key={zone.id}
                    type="button"
                    onClick={() => toggleZone(zone.id)}
                    className={`rounded-full px-3 py-1.5 text-label-md transition-colors ${
                      selected ? 'bg-primary text-on-primary' : 'bg-surface-container-lowest text-on-surface-variant'
                    }`}
                  >
                    {zone.name}
                  </button>
                )
              })}
            </div>
          </div>

          {error && <p className="text-body-sm text-error">{error}</p>}

          <button
            type="submit"
            disabled={saving}
            className="flex h-[52px] w-full items-center justify-center gap-2 rounded-full bg-primary text-label-lg text-on-primary shadow-md transition-all hover:bg-primary-container active:scale-[0.98] disabled:opacity-60"
          >
            <span>{saving ? 'Guardando…' : 'Guardar y continuar'}</span>
            <Icon name="arrow_forward" className="text-[20px]" />
          </button>
        </fieldset>
      </form>

      <div className="mt-4 flex items-center justify-center pb-6">
        <button type="button" onClick={onDone} className="text-label-md text-on-surface-variant hover:underline">
          Hacerlo más tarde
        </button>
      </div>
    </AppScreen>
  )
}

export function RegisterPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const { zones } = usePublicZones()

  const fullNameId = useId()
  const emailId = useId()
  const phoneId = useId()
  const zoneFieldId = useId()
  const passwordId = useId()

  const [step, setStep] = useState<'profile' | 'coverage'>('profile')
  const [pendingSession, setPendingSession] = useState<Session | null>(null)
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [role, setRole] = useState<Role>('CLIENT')
  const [zoneId, setZoneId] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function goHome() {
    navigate(paths.home, { replace: true })
  }

  // recién acá actualizamos el contexto de auth (signIn de verdad): si lo hiciéramos antes,
  // GuestOnly (que envuelve esta ruta) redirige a home apenas detecta sesión y desmonta la
  // página con el paso 2 todavía pendiente
  function finishWalkerSetup() {
    if (pendingSession) signIn(pendingSession)
    goHome()
  }

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
      })
      // TODO: mostrar un modal de bienvenida acá antes de navegar ("Cuenta creada, ya podés gestionar tus paseos")
      if (role === 'WALKER') {
        // persiste el token para que el PUT del paso 2 quede autenticado, sin disparar
        // todavía el redirect de GuestOnly (ver finishWalkerSetup)
        writeSession(session)
        setPendingSession(session)
        setStep('coverage')
      } else {
        signIn(session)
        goHome()
      }
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

  if (step === 'coverage') {
    return <WalkerCoverageStep zones={zones} onDone={finishWalkerSetup} />
  }

  return (
    <AppScreen>
      <div className="mt-4 mb-4">
        <IconButton aria-label="Volver" onClick={() => navigate(-1)}>
          <Icon name="arrow_back" className="text-[20px]" />
        </IconButton>
      </div>

      <div className="mb-6">
        <div className="mb-1.5 flex items-center gap-2">
          <span className="text-headline-lg font-bold text-on-surface">Creá tu cuenta</span>
          <span className="text-[26px]">🐾</span>
        </div>
        <p className="text-body-md text-on-surface-variant">
          Conectá con paseadores y salidas grupales en tu barrio.
        </p>
      </div>

      <div className="mb-6">
        <span className="mb-2.5 block text-label-md font-semibold text-on-surface">Elegí tu perfil</span>
        <div className="grid grid-cols-2 gap-3">
          <RoleCard
            selected={role === 'CLIENT'}
            onClick={() => setRole('CLIENT')}
            icon="pets"
            iconFilled
            title="Busco paseos"
            subtitle="Dueño o familia perruna"
          />
          <RoleCard
            selected={role === 'WALKER'}
            onClick={() => setRole('WALKER')}
            icon="hiking"
            title="Soy paseador"
            subtitle="Coordino salidas de grupo"
          />
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <fieldset disabled={submitting} className="space-y-4">
          <TextField
            id={fullNameId}
            label="Nombre y apellido"
            icon="person"
            autoComplete="name"
            placeholder="Martina Rossi"
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            required
          />

          <TextField
            id={emailId}
            label="Correo electrónico"
            icon="alternate_email"
            type="email"
            autoComplete="email"
            placeholder="martina@ejemplo.com"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />

          <TextField
            id={phoneId}
            label="Teléfono / WhatsApp (opcional)"
            icon="chat"
            type="tel"
            autoComplete="tel"
            placeholder="+54 9 11 4521-8899"
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
          />

          <SelectField
            id={zoneFieldId}
            label="Zona donde vivís"
            icon="location_on"
            iconFilled
            iconClassName="text-primary"
            value={zoneId}
            onChange={(event) => setZoneId(event.target.value)}
            disabled={zones.length === 0}
            required
          >
            <option value="">Seleccioná un barrio</option>
            {zones.map((zone) => (
              <option key={zone.id} value={zone.id}>
                {zone.name} — {zone.city}
              </option>
            ))}
          </SelectField>

          <TextField
            id={passwordId}
            label="Contraseña"
            icon="lock"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            placeholder="Al menos 8 caracteres"
            minLength={8}
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

          <div className="mt-1 flex items-center gap-3 rounded-xl bg-surface-container-low p-3 shadow-sm">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-secondary-container/30 text-on-secondary-container">
              <Icon name="verified_user" className="text-[18px]" />
            </div>
            <p className="text-caption text-on-surface-variant">
              Tu información queda protegida: nunca la compartimos con otros usuarios de la red.
            </p>
          </div>

          {error && <p className="text-body-sm text-error">{error}</p>}

          <div className="pt-2">
            <button
              type="submit"
              className="flex h-[52px] w-full items-center justify-center gap-2 rounded-full bg-primary text-label-lg text-on-primary shadow-md transition-all hover:bg-primary-container active:scale-[0.98] disabled:opacity-60"
            >
              <span>{submitting ? 'Creando cuenta…' : 'Crear cuenta'}</span>
              <Icon name="arrow_forward" className="text-[20px]" />
            </button>
          </div>
        </fieldset>
      </form>

      <div className="mt-5 flex items-center justify-center pb-6">
        <p className="text-body-sm text-on-surface-variant">
          ¿Ya tenés cuenta?{' '}
          <Link to={paths.login} className="ml-1 text-label-md text-primary hover:underline">
            Ingresar
          </Link>
        </p>
      </div>
    </AppScreen>
  )
}
