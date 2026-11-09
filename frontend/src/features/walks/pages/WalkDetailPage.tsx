import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth'
import { bookingError, joinWalk } from '@/features/bookings/api/bookings'
import { ZoneCoverageMap, useZones } from '@/features/map'
import { loadMyPets, type Pet } from '@/features/pets/api/pets'
import { WalkReviewsSection } from '@/features/reviews/components/WalkReviewsSection'
import { PageHeader } from '@/shared/components/PageHeader'
import { formatWalkDate } from '../api/search'
import { walkStatusLabels, type WalkDetails } from '../api/walkReadContract'
import { loadWalkDetail } from '../api/walks'
import { WalkStateActions } from '../components/WalkStateActions'
import { useWalkResource } from '../hooks/useWalkResource'

function JoinWalkForm({ walkId, onJoined }: { walkId: string; onJoined: () => void }) {
  const [pets, setPets] = useState<Pet[] | null>(null)
  const [petsError, setPetsError] = useState<string | null>(null)
  const [petId, setPetId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const posting = useRef(false)

  useEffect(() => {
    const controller = new AbortController()
    loadMyPets(controller.signal).then((result) => {
      setPets(result)
      if (result.length === 1) setPetId(result[0].id)
    }).catch((failure: unknown) => {
      if (controller.signal.aborted) return
      setPetsError(failure instanceof Error ? failure.message : 'No pudimos cargar tus mascotas.')
    })
    return () => controller.abort()
  }, [])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (posting.current || !petId) return
    posting.current = true
    setSubmitting(true)
    setError(null)
    try {
      await joinWalk(walkId, petId)
      onJoined()
    } catch (failure) {
      setError(bookingError(failure))
    } finally {
      posting.current = false
      setSubmitting(false)
    }
  }

  if (petsError) return <p role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">{petsError}</p>
  if (!pets) return <p role="status" className="text-sm text-slate-600">Cargando tus mascotas…</p>
  if (!pets.length) return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 text-sm text-slate-600">
      <p>Necesitás una mascota registrada para reservar.</p>
      <Link to={paths.newPet} className="mt-2 inline-block font-medium text-blue-700 underline">Agregar mascota</Link>
    </div>
  )

  return (
    <form onSubmit={handleSubmit} className="space-y-3 rounded-xl border border-slate-200 bg-white p-4">
      <label className="block text-sm font-medium">
        Mascota
        <select value={petId} onChange={(event) => setPetId(event.target.value)} required
          className="mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2">
          <option value="" disabled>Elegí una mascota</option>
          {pets.map((pet) => <option key={pet.id} value={pet.id}>{pet.name}</option>)}
        </select>
      </label>
      {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      <button type="submit" disabled={submitting || !petId}
        className="rounded-lg bg-emerald-700 px-4 py-2 font-semibold text-white disabled:opacity-50">
        {submitting ? 'Reservando…' : 'Reservar'}
      </button>
    </form>
  )
}

export function WalkDetailPage() {
  const { walkId } = useParams<{ walkId: string }>()
  const { session } = useAuth()
  const location = useLocation()
  const request = useCallback((signal: AbortSignal) => loadWalkDetail(walkId ?? '', signal), [walkId])
  const { data: fetchedWalk, loading, error, reload } = useWalkResource(`${session?.token ?? ''}:${walkId}`, !!session, request)
  // al llegar desde "Publicar paseo" ya tenemos el detalle: evita el flash de "Cargando…" en el
  // primer render. El hook igual revalida contra el servidor y lo reemplaza cuando responde.
  const preloaded = (location.state as { walk?: WalkDetails } | null)?.walk
  const walk = fetchedWalk ?? (preloaded?.id === walkId ? preloaded : undefined)
  const catalog = useZones()
  const zone = catalog.zones.find(item => item.id === walk?.zoneId)
  const ownWalk = !!walk && session?.user.role === 'WALKER' && walk.walker.id === session.user.id

  if (!session) return (
    <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6">
      <PageHeader title="Ingresá para ver este paseo" description="Buscar paseos y ver el detalle requiere una cuenta." />
      <Link to={paths.login} state={{ from: location }} className="inline-block rounded-lg bg-blue-700 px-4 py-2 font-semibold text-white">Ingresar</Link>
      <Link to={paths.walks} className="ml-4 text-sm font-medium text-blue-700 underline">Volver a la búsqueda</Link>
    </section>
  )

  return (
    <section className="space-y-6" aria-busy={loading}>
      <Link to={ownWalk ? paths.myWalks : paths.walks} className="text-sm font-medium text-blue-700 underline">{ownWalk ? 'Volver a mis paseos' : 'Volver a la búsqueda'}</Link>
      {loading && !walk && <p role="status">Cargando el detalle del paseo…</p>}
      {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-5"><p>{error}</p><button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar detalle</button></div>}
      {walk && <>
        <div className="rounded-2xl border border-blue-100 bg-blue-50 p-6"><p className="text-sm font-semibold text-blue-800">{walkStatusLabels[walk.status]}</p><h1 className="mt-2 text-3xl font-bold">Paseo en {walk.zoneName}</h1><p className="mt-3 break-words text-slate-700">Con {walk.walker.name}</p></div>
        <div className="grid items-start gap-6 md:grid-cols-2">
          <div className="space-y-5 rounded-2xl border border-slate-200 bg-white p-6">
            <dl className="space-y-4">
              <div><dt className="text-sm text-slate-500">Inicio</dt><dd className="mt-1 font-semibold">{formatWalkDate(walk.startTime)}</dd></div>
              <div><dt className="text-sm text-slate-500">Finalización prevista</dt><dd className="mt-1 font-semibold">{formatWalkDate(walk.endTime)}</dd></div>
              <div><dt className="text-sm text-slate-500">Duración</dt><dd className="mt-1">{walk.durationMinutes} minutos</dd></div>
              <div><dt className="text-sm text-slate-500">Punto de encuentro</dt><dd className="mt-1 break-words">{walk.meetingPoint}</dd></div>
              <div><dt className="text-sm text-slate-500">Lugares</dt><dd className="mt-1">{walk.bookedSpots} ocupados de {walk.capacity} · {walk.availableSpots} libres</dd></div>
              <div><dt className="text-sm text-slate-500">Precio por mascota</dt><dd className="mt-1 text-xl font-bold text-blue-800">{walk.pricePerPet.toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })}</dd></div>
              <div><dt className="text-sm text-slate-500">Valoraciones del paseador</dt><dd className="mt-1">{walk.walker.reviewCount ? `${walk.walker.ratingAvg.toLocaleString('es-AR')} / 5 · ${walk.walker.reviewCount} reseñas` : 'Todavía sin reseñas'}</dd></div>
            </dl>
            <p className="text-xs text-slate-500">Todos los horarios corresponden a CABA.</p>
          </div>
          <aside className="rounded-2xl border border-slate-200 bg-white p-5"><h2 className="mb-3 font-bold">Ubicación orientativa del barrio</h2>{zone ? <ZoneCoverageMap zones={[zone]} /> : <p className="text-sm text-slate-600">{catalog.loading ? 'Cargando el barrio…' : 'No pudimos representar este barrio en el mapa.'}</p>}<p className="mt-3 text-xs text-slate-500">El encuentro se realiza en el punto publicado. El mapa no muestra recorridos ni ubicación en tiempo real.</p></aside>
        </div>
        {ownWalk && <WalkStateActions walkId={walk.id} status={walk.status} onChanged={reload} />}
        {!ownWalk && session.user.role === 'CLIENT' && (
          walk.status === 'PUBLISHED' && walk.availableSpots > 0
            ? <JoinWalkForm walkId={walk.id} onJoined={reload} />
            : <p className="rounded-xl border border-slate-200 bg-white p-4 text-sm text-slate-600">
                {walk.status === 'FULL' ? 'Este paseo ya no tiene lugares disponibles.' : 'Este paseo ya no admite inscripciones.'}
              </p>
        )}
        {walk.status === 'COMPLETED' && (
          <WalkReviewsSection walkId={walk.id} canReview={!ownWalk && session.user.role === 'CLIENT'} />
        )}
      </>}
    </section>
  )
}
