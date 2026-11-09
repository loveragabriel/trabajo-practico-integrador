import { useRef, useState, type SubmitEvent } from 'react'
import { generatePath, Link, useNavigate } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth'
import { ZoneSelect } from '@/features/map'
import { ApiRequestError } from '@/shared/api/client'
import { PageHeader } from '@/shared/components/PageHeader'
import { loadWalkPublicationOptions, publishWalk, type WalkPublicationOptions } from '../api/walkPublication'
import { useWalkResource } from '../hooks/useWalkResource'

const fieldClass = 'mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2'
const earliestStart = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'America/Argentina/Buenos_Aires', year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
})

function minimumStart() {
  const instant = new Date(Math.ceil((Date.now() + 2 * 60 * 60 * 1000) / 60000) * 60000)
  const parts = Object.fromEntries(earliestStart.formatToParts(instant).map(part => [part.type, part.value]))
  return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}`
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

function PublicationForm({ options }: { options: WalkPublicationOptions }) {
  const navigate = useNavigate()
  const [zoneId, setZoneId] = useState(options.zones.length === 1 ? options.zones[0].id : '')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const posting = useRef(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (posting.current) return
    const fields = new FormData(event.currentTarget)
    const meetingPoint = String(fields.get('meetingPoint') ?? '').trim()
    if (!meetingPoint) {
      setError(validationMessages.meetingPoint)
      return
    }

    posting.current = true
    setSubmitting(true)
    setError(null)
    try {
      const id = await publishWalk({
        zoneId,
        startTime: String(fields.get('startTime') ?? ''),
        durationMinutes: Number(fields.get('durationMinutes')),
        capacity: Number(fields.get('capacity')),
        pricePerPet: Number(fields.get('pricePerPet')),
        meetingPoint,
      })
      navigate(generatePath(paths.walkDetail, { walkId: id }), { replace: true })
    } catch (failure) {
      setError(publicationError(failure))
    } finally {
      posting.current = false
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4 rounded-xl border border-slate-200 bg-white p-4 sm:p-6">
      <fieldset disabled={submitting} className="space-y-4">
        <legend className="sr-only">Datos del paseo</legend>
        <ZoneSelect zones={options.zones} value={zoneId} onChange={setZoneId} label="Barrio de servicio" required />
        <label className="block text-sm font-medium">
          Inicio (hora de Buenos Aires)
          <input name="startTime" type="datetime-local" min={minimumStart()} required className={fieldClass} />
        </label>
        <p className="text-sm text-slate-600">Elegí un horario con al menos dos horas de anticipación.</p>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block text-sm font-medium">
            Duración en minutos
            <input name="durationMinutes" type="number" min={30} max={2147483647} step={15} defaultValue={30} required className={fieldClass} />
          </label>
          <label className="block text-sm font-medium">
            Cupo de mascotas
            <input name="capacity" type="number" min={1} max={2147483647} step={1} defaultValue={options.defaultCapacity ?? ''} required className={fieldClass} />
          </label>
        </div>
        <label className="block text-sm font-medium">
          Precio por mascota (ARS)
          <input name="pricePerPet" type="number" min={0} max={99999999.99} step={0.01} defaultValue={options.defaultPrice ?? ''} required className={fieldClass} />
        </label>
        <label className="block text-sm font-medium">
          Punto de encuentro
          <textarea name="meetingPoint" rows={3} required className={fieldClass} />
        </label>
      </fieldset>
      {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      <button type="submit" disabled={submitting} className="rounded-lg bg-emerald-700 px-4 py-2 font-semibold text-white disabled:opacity-50">
        {submitting ? 'Publicando…' : 'Publicar paseo'}
      </button>
    </form>
  )
}

export function PublishWalkPage() {
  const { session } = useAuth()
  const { data, loading, error, reload } = useWalkResource(session?.token ?? '', !!session, loadWalkPublicationOptions)

  return (
    <section className="max-w-2xl space-y-4">
      <PageHeader title="Publicar paseo" description="Definí el horario, barrio, cupo y precio de la salida." />
      <Link to={paths.myWalks} className="text-sm font-medium text-emerald-700 underline">Volver a Mis paseos</Link>
      {loading && <p role="status">Cargando tus barrios de servicio…</p>}
      {error && <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-4">
        <p>{error}</p>
        <button type="button" onClick={reload} className="mt-2 font-medium underline">Reintentar</button>
      </div>}
      {data && (data.zones.length
        ? <PublicationForm options={data} />
        : <div className="rounded-lg border border-slate-200 bg-white p-4">
          <p>Necesitás al menos un barrio de servicio para publicar un paseo.</p>
          <Link to={`${paths.profile}#cobertura`} className="mt-2 inline-block font-medium text-emerald-700 underline">Configurar cobertura</Link>
        </div>)}
    </section>
  )
}
