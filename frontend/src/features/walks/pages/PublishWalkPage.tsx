import { useId, useRef, useState, type SubmitEvent } from 'react'
import { generatePath, Link, useNavigate } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth'
import { ApiRequestError } from '@/shared/api/client'
import { Icon } from '@/shared/components/Icon'
import { SelectField } from '@/shared/components/SelectField'
import { TextField } from '@/shared/components/TextField'
import { loadWalkPublicationOptions, publishWalk, type WalkPublicationOptions } from '../api/walkPublication'
import { useWalkResource } from '../hooks/useWalkResource'

const durationOptions = [30, 45, 60, 90]
const MIN_CAPACITY = 1
// tope solo de UI para que el stepper y los íconos de lugares no se vuelvan absurdos:
// RN-15 no impone un máximo, el backend acepta cualquier entero >= 1
const MAX_CAPACITY = 30
const MAX_PAW_SLOTS = 12

const earliestStart = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'America/Argentina/Buenos_Aires', year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
})

function minimumStartDate() {
  const instant = new Date(Math.ceil((Date.now() + 2 * 60 * 60 * 1000) / 60000) * 60000)
  const parts = Object.fromEntries(earliestStart.formatToParts(instant).map(part => [part.type, part.value]))
  return `${parts.year}-${parts.month}-${parts.day}`
}

const validationMessages: Record<string, string> = {
  zoneId: 'Elegí un barrio dentro de tus zonas de servicio.',
  startTime: 'Revisá la fecha y hora de inicio: necesitás al menos dos horas de anticipación.',
  durationMinutes: 'Ingresá una duración de al menos 30 minutos, en múltiplos de 15.',
  capacity: 'Ingresá un cupo entero mayor que cero.',
  pricePerPet: 'Ingresá un precio válido, con hasta dos decimales.',
  meetingPoint: 'Indicá el punto de encuentro.',
}

function publicationError(error: unknown) {
  if (error instanceof ApiRequestError) {
    if (error.status === 409) return 'Ya tenés un paseo que coincide con ese horario. Elegí otro.'
    if (error.status === 403) return 'Solo un paseador activo puede publicar.'
    if (error.status === 400) {
      const field = error.fieldErrors?.[0]?.field
      return field && Object.hasOwn(validationMessages, field)
        ? validationMessages[field]
        : 'Revisá los datos del paseo e intentá nuevamente.'
    }
    return 'No pudimos confirmar la publicación. Revisá Mis paseos antes de volver a enviarla.'
  }
  return error instanceof Error ? error.message : 'No pudimos publicar el paseo.'
}

function capacityBadge(capacity: number) {
  if (capacity <= 3) return { label: 'Grupo ultra-personalizado', className: 'bg-primary-fixed/40 text-primary' }
  if (capacity <= 5) return { label: 'Grupo equilibrado', className: 'bg-secondary-container/30 text-secondary' }
  return { label: 'Manada grande', className: 'bg-surface-variant text-on-surface-variant' }
}

function PublicationForm({ options }: { options: WalkPublicationOptions }) {
  const navigate = useNavigate()
  const detailId = useId()
  const priceId = useId()

  const [zoneId, setZoneId] = useState(options.zones.length === 1 ? options.zones[0].id : '')
  const [date, setDate] = useState('')
  const [time, setTime] = useState('')
  const [durationMinutes, setDurationMinutes] = useState(60)
  const [capacity, setCapacity] = useState(options.defaultCapacity ?? 1)
  const [pricePerPet, setPricePerPet] = useState(options.defaultPrice != null ? String(options.defaultPrice) : '')
  const [meetingPoint, setMeetingPoint] = useState('')
  const [meetingDetail, setMeetingDetail] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const posting = useRef(false)

  const badge = capacityBadge(capacity)
  const priceValue = Number(pricePerPet) || 0
  const potentialEarnings = priceValue * capacity

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (posting.current) return
    const point = meetingPoint.trim()
    if (!point) {
      setError(validationMessages.meetingPoint)
      return
    }

    posting.current = true
    setSubmitting(true)
    setError(null)
    try {
      const created = await publishWalk({
        zoneId,
        startTime: `${date}T${time}`,
        durationMinutes,
        capacity,
        pricePerPet: priceValue,
        // el backend solo tiene un campo meetingPoint: la referencia opcional se concatena en vez
        // de inventar una columna nueva
        meetingPoint: meetingDetail.trim() ? `${point} — ${meetingDetail.trim()}` : point,
      })
      navigate(generatePath(paths.walkPublished, { walkId: created.id }), { replace: true, state: { walk: created } })
    } catch (failure) {
      setError(publicationError(failure))
    } finally {
      posting.current = false
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <fieldset disabled={submitting} className="space-y-4">
        <legend className="sr-only">Datos del paseo</legend>

        <div className="space-y-4 rounded-2xl bg-surface-container-lowest p-5 shadow-sm">
          <div className="flex items-center gap-2">
            <Icon name="calendar_clock" filled className="text-[20px] text-primary" />
            <h2 className="text-title-sm font-semibold text-on-surface">Fecha y horario</h2>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <TextField label="Día del paseo" icon="calendar_today" type="date" min={minimumStartDate()}
              value={date} onChange={(event) => setDate(event.target.value)} required />
            <TextField label="Hora de inicio" icon="schedule" type="time"
              value={time} onChange={(event) => setTime(event.target.value)} required />
          </div>
          <p className="text-caption text-on-surface-variant">Necesitás al menos dos horas de anticipación.</p>

          <div className="space-y-2">
            <span className="block text-label-md font-semibold text-on-surface">Duración de la salida</span>
            <div className="grid grid-cols-4 gap-2">
              {durationOptions.map((minutes) => (
                <button key={minutes} type="button" onClick={() => setDurationMinutes(minutes)}
                  aria-pressed={durationMinutes === minutes}
                  className={`flex h-10 items-center justify-center rounded-full text-label-md transition-all ${
                    durationMinutes === minutes
                      ? 'bg-primary-container font-semibold text-on-primary shadow-sm'
                      : 'bg-surface-container-low text-on-surface-variant'
                  }`}>
                  {minutes} min
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="space-y-4 rounded-2xl bg-surface-container-lowest p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Icon name="location_on" filled className="text-[20px] text-primary" />
              <h2 className="text-title-sm font-semibold text-on-surface">Zona y encuentro</h2>
            </div>
            {zoneId && (
              <span className="flex items-center gap-1 rounded-full bg-primary-fixed px-2.5 py-0.5 text-label-sm font-medium text-on-primary-fixed">
                <span className="h-1.5 w-1.5 rounded-full bg-primary" />
                Zona elegida
              </span>
            )}
          </div>

          <SelectField label="Barrio de servicio" icon="map" value={zoneId}
            onChange={(event) => setZoneId(event.target.value)} required>
            <option value="" disabled>Elegí un barrio</option>
            {options.zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name} — {zone.city}</option>)}
          </SelectField>

          <TextField label="Punto de encuentro" icon="explore" placeholder="Ej: Plaza Benito Nazar"
            value={meetingPoint} onChange={(event) => setMeetingPoint(event.target.value)} required />

          <div className="space-y-1.5">
            <label htmlFor={detailId} className="block text-label-md font-semibold text-on-surface">
              Detalle o referencia (opcional)
            </label>
            <div className="flex items-start gap-2 rounded-lg bg-surface-container-low p-3 transition-all focus-within:bg-surface-container-lowest focus-within:shadow-[0_0_0_2px_var(--color-primary)]">
              <Icon name="info" className="mt-0.5 text-[18px] text-on-surface-variant" />
              <textarea id={detailId} rows={2} value={meetingDetail} onChange={(event) => setMeetingDetail(event.target.value)}
                placeholder="Indicaciones para encontrarse con facilidad…"
                className="w-full resize-none bg-transparent text-body-sm text-on-surface outline-none placeholder:text-outline" />
            </div>
          </div>
        </div>

        <div className="space-y-4 rounded-2xl bg-surface-container-lowest p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Icon name="pets" filled className="text-[20px] text-secondary" />
              <h2 className="text-title-sm font-semibold text-on-surface">Capacidad de la manada</h2>
            </div>
            <span className={`rounded-full px-2.5 py-0.5 text-label-sm font-medium ${badge.className}`}>{badge.label}</span>
          </div>

          <div className="flex flex-col items-center space-y-3 rounded-xl bg-surface-container-low p-4">
            <span className="text-label-sm font-medium uppercase tracking-wider text-on-surface-variant">Cupo máximo de perros</span>
            <div className="flex w-full max-w-[240px] items-center justify-between px-2">
              <button type="button" aria-label="Disminuir cupo" disabled={capacity <= MIN_CAPACITY}
                onClick={() => setCapacity((value) => Math.max(MIN_CAPACITY, value - 1))}
                className="flex h-11 w-11 items-center justify-center rounded-full bg-surface-container-lowest text-primary shadow-sm transition-transform active:scale-95 disabled:opacity-40">
                <Icon name="remove" className="text-[20px]" />
              </button>
              <div className="flex flex-col items-center">
                <span className="text-headline-lg font-bold tracking-tight text-primary">{capacity}</span>
                <span className="-mt-1 text-label-sm font-medium text-on-surface-variant">perros max.</span>
              </div>
              <button type="button" aria-label="Aumentar cupo" disabled={capacity >= MAX_CAPACITY}
                onClick={() => setCapacity((value) => Math.min(MAX_CAPACITY, value + 1))}
                className="flex h-11 w-11 items-center justify-center rounded-full bg-surface-container-lowest text-primary shadow-sm transition-transform active:scale-95 disabled:opacity-40">
                <Icon name="add" className="text-[20px]" />
              </button>
            </div>
            <div className="mx-auto grid w-fit grid-cols-4 gap-2 pt-1">
              {Array.from({ length: Math.min(capacity, MAX_PAW_SLOTS) }, (_, index) => (
                <div key={index} className="flex h-9 w-9 items-center justify-center rounded-full bg-surface-container-lowest text-primary shadow-xs">
                  <Icon name="pets" filled className="text-[18px]" />
                </div>
              ))}
              {capacity > MAX_PAW_SLOTS && (
                <span className="flex h-9 w-9 items-center justify-center text-label-sm font-medium text-on-surface-variant">+{capacity - MAX_PAW_SLOTS}</span>
              )}
            </div>
          </div>
        </div>

        <div className="space-y-4 rounded-2xl bg-surface-container-lowest p-5 shadow-sm">
          <div className="flex items-center gap-2">
            <Icon name="payments" filled className="text-[20px] text-primary" />
            <h2 className="text-title-sm font-semibold text-on-surface">Tarifa por mascota</h2>
          </div>

          <div className="space-y-1.5">
            <label htmlFor={priceId} className="block text-label-md font-semibold text-on-surface">Precio por perro (ARS)</label>
            <div className="flex h-[50px] items-center gap-2 rounded-lg bg-surface-container-low px-3.5 transition-all focus-within:bg-surface-container-lowest focus-within:shadow-[0_0_0_2px_var(--color-primary)]">
              <span className="text-title-sm font-bold text-primary">$</span>
              <input id={priceId} type="number" min={0} max={99999999.99} step={0.01} value={pricePerPet}
                onChange={(event) => setPricePerPet(event.target.value)} required
                className="w-full bg-transparent text-title-sm font-semibold text-on-surface outline-none" />
              <span className="text-label-sm font-semibold text-outline">ARS</span>
            </div>
          </div>

          <div className="flex items-center justify-between rounded-xl bg-primary-fixed/30 p-3.5">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-fixed text-primary">
                <Icon name="trending_up" className="text-[18px]" />
              </div>
              <div className="flex flex-col">
                <span className="text-label-sm text-on-primary-fixed-variant">Ingreso potencial estimado</span>
                <span className="text-caption text-outline">con {capacity} cupos cubiertos</span>
              </div>
            </div>
            <span className="text-title-md font-bold tracking-tight text-primary">
              {potentialEarnings.toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })}
            </span>
          </div>
        </div>
      </fieldset>

      {error && <p role="alert" className="rounded-xl bg-error-container p-3 text-body-sm text-on-error-container">{error}</p>}

      <button type="submit" disabled={submitting}
        className="flex h-[52px] w-full items-center justify-center gap-2 rounded-full bg-primary-container text-label-lg font-semibold text-on-primary shadow-sm transition-all active:scale-[0.98] disabled:opacity-60">
        <Icon name="add_task" className="text-[20px]" />
        {submitting ? 'Publicando…' : 'Publicar salida grupal'}
      </button>
    </form>
  )
}

export function PublishWalkPage() {
  const { session } = useAuth()
  const { data, loading, error, reload } = useWalkResource(session?.token ?? '', !!session, loadWalkPublicationOptions)

  return (
    <section className="mx-auto max-w-md space-y-5 pb-8">
      <div className="flex flex-col">
        <div className="mb-1 flex items-center gap-1.5 text-primary">
          <Icon name="groups" filled className="text-[18px]" />
          <span className="text-label-sm font-semibold uppercase tracking-wider">Organizador de manada</span>
        </div>
        <h1 className="text-headline-md font-bold text-on-surface">Nueva salida grupal</h1>
        <p className="mt-1.5 text-body-sm leading-relaxed text-on-surface-variant">
          Definí el horario, cupos y punto de encuentro para que los dueños de tu zona puedan sumarse con tranquilidad.
        </p>
      </div>

      <Link to={paths.myWalks} className="text-label-md text-primary underline-offset-2 hover:underline">Volver a Mis paseos</Link>

      {loading && <p role="status" className="text-body-sm text-on-surface-variant">Cargando tus barrios de servicio…</p>}
      {error && (
        <div role="alert" className="rounded-xl bg-error-container p-4">
          <p className="text-body-sm text-on-error-container">{error}</p>
          <button type="button" onClick={reload} className="mt-2 text-label-md font-semibold text-on-error-container underline">Reintentar</button>
        </div>
      )}
      {data && (data.zones.length
        ? <PublicationForm options={data} />
        : <div className="rounded-xl bg-surface-container-lowest p-4 shadow-sm">
          <p className="text-body-sm text-on-surface">Necesitás al menos un barrio de servicio para publicar un paseo.</p>
          <Link to={paths.profile} className="mt-2 inline-block text-label-md font-semibold text-primary underline">Ver mi perfil</Link>
        </div>)}
    </section>
  )
}
