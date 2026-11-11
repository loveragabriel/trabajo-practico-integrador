import { useCallback, useEffect, useState } from 'react'
import { generatePath, Link, useLocation, useParams } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth'
import { ZoneCoverageMap, useZones } from '@/features/map'
import { petSizeLabels } from '@/features/pets/api/pets'
import { WalkReviewsSection } from '@/features/reviews/components/WalkReviewsSection'
import { Icon } from '@/shared/components/Icon'
import { PageHeader } from '@/shared/components/PageHeader'
import { formatWalkDate } from '../api/search'
import { walkStatusBadgeClass, walkStatusLabels, type WalkDetails } from '../api/walkReadContract'
import { loadJoinedPets, loadWalkDetail, type JoinedPet } from '../api/walks'
import { ClientWalkDetail } from '../components/ClientWalkDetail'
import { WalkStateActions } from '../components/WalkStateActions'
import { useWalkResource } from '../hooks/useWalkResource'

function WalkOwnerSummaryCard({ walk }: { walk: WalkDetails }) {
  return (
    <section className="flex flex-col gap-4 rounded-xl bg-surface-container-lowest p-5 shadow-sm">
      <div className="flex items-start justify-between gap-2">
        <div className="flex flex-col gap-1.5">
          <div className="flex flex-wrap items-center gap-2">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-primary-fixed/40 px-2.5 py-1 text-label-md text-primary">
              <Icon name="schedule" className="text-[16px]" />
              {formatWalkDate(walk.startTime)}
            </span>
            <span className={`rounded-full px-2.5 py-1 text-label-sm font-semibold ${walkStatusBadgeClass[walk.status]}`}>
              {walkStatusLabels[walk.status]}
            </span>
          </div>
          <h1 className="text-headline-sm font-bold text-on-surface">Paseo en {walk.zoneName}</h1>
        </div>
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-surface-container-low text-primary">
          <Icon name="pets" className="text-[22px]" />
        </div>
      </div>

      <div className="flex items-start gap-2.5 rounded-lg bg-surface-container-low/70 p-3">
        <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-surface-container-highest/60 text-primary">
          <Icon name="location_on" className="text-[18px]" />
        </div>
        <div className="flex min-w-0 flex-col">
          <span className="text-label-sm uppercase tracking-wider text-on-surface-variant">Punto de encuentro</span>
          <span className="break-words text-title-sm font-medium text-on-surface">{walk.meetingPoint}</span>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
        <span className="inline-flex items-center gap-1.5 rounded-full bg-primary-fixed/50 px-3 py-1 text-label-md text-primary">
          <span className="h-2 w-2 rounded-full bg-primary" />
          {walk.bookedSpots} de {walk.capacity} perros inscriptos
        </span>
        <span className="text-label-md text-secondary">
          {walk.availableSpots > 0
            ? `${walk.availableSpots} cupos libres (${walk.pricePerPet.toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })} c/u)`
            : 'Sin cupos libres'}
        </span>
      </div>
    </section>
  )
}

function JoinedPetCard({ pet }: { pet: JoinedPet }) {
  const whatsappPhone = pet.owner.phone?.replace(/\D/g, '')
  return (
    <article className="flex flex-col gap-3.5 rounded-xl bg-surface-container-lowest p-4 shadow-sm">
      <div className="flex items-start gap-3">
        <div className="relative shrink-0">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-primary-fixed text-primary">
            <Icon name="pets" filled className="text-[26px]" />
          </div>
          <span className="absolute bottom-0 right-0 flex h-4 w-4 items-center justify-center rounded-full bg-primary text-on-primary">
            <Icon name="check" filled className="text-[10px]" />
          </span>
        </div>
        <div className="flex min-w-0 flex-1 flex-col">
          <div className="flex items-center justify-between gap-1">
            <h4 className="truncate text-title-sm font-semibold text-on-surface">{pet.petName}</h4>
            {pet.size && <span className="shrink-0 rounded-full bg-surface-container px-2 py-0.5 text-label-sm text-on-surface-variant">{petSizeLabels[pet.size]}</span>}
          </div>
          <p className="text-body-sm text-on-surface-variant">{pet.breed ?? 'Raza sin especificar'}</p>
          <div className="mt-1 flex items-center justify-between gap-2 pt-1">
            <div className="flex min-w-0 items-center gap-1.5">
              <Icon name="person" className="text-[16px] text-on-surface-variant" />
              <span className="truncate text-label-md font-medium text-on-surface">{pet.owner.name}</span>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              <a aria-label={`Llamar a ${pet.owner.name}`} href={pet.owner.phone ? `tel:${pet.owner.phone}` : undefined}
                className={`flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-primary transition-transform active:scale-95 ${pet.owner.phone ? '' : 'pointer-events-none opacity-40'}`}>
                <Icon name="call" className="text-[18px]" />
              </a>
              <a aria-label={`Enviar WhatsApp a ${pet.owner.name}`} href={whatsappPhone ? `https://wa.me/${whatsappPhone}` : undefined}
                className={`flex h-8 w-8 items-center justify-center rounded-full bg-primary text-on-primary shadow-sm transition-transform active:scale-95 ${whatsappPhone ? '' : 'pointer-events-none opacity-40'}`}>
                <Icon name="chat" className="text-[18px]" />
              </a>
            </div>
          </div>
        </div>
      </div>
      {pet.notes && (
        <div className="flex items-start gap-2 rounded-lg bg-secondary-fixed/30 p-2.5 shadow-inner">
          <Icon name="info" className="mt-0.5 shrink-0 text-[18px] text-secondary" />
          <p className="text-caption font-medium leading-snug text-on-secondary-fixed">
            <span className="font-semibold text-secondary">Nota de {pet.owner.name}:</span> {pet.notes}
          </p>
        </div>
      )}
    </article>
  )
}

function JoinedPetsRoster({ walkId }: { walkId: string }) {
  const [pets, setPets] = useState<JoinedPet[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    loadJoinedPets(walkId, controller.signal).then(setPets).catch((failure: unknown) => {
      if (controller.signal.aborted) return
      setError(failure instanceof Error ? failure.message : 'No pudimos cargar las mascotas de este paseo.')
    })
    return () => controller.abort()
  }, [walkId])

  if (error) return <p role="alert" className="rounded-xl bg-error-container p-4 text-body-sm text-on-error-container">{error}</p>
  if (!pets) return <p role="status" className="text-body-sm text-on-surface-variant">Cargando las mascotas de esta salida…</p>
  if (!pets.length) return (
    <div className="rounded-2xl bg-surface-container-lowest p-6 text-center shadow-sm">
      <p className="text-body-sm text-on-surface-variant">Todavía no se sumó ninguna mascota a esta salida.</p>
    </div>
  )

  return <div className="flex flex-col gap-3.5">{pets.map((pet) => <JoinedPetCard key={pet.petId} pet={pet} />)}</div>
}

export function WalkDetailPage() {
  const { walkId } = useParams<{ walkId: string }>()
  const { session } = useAuth()
  const location = useLocation()
  const request = useCallback((signal: AbortSignal) => loadWalkDetail(walkId ?? '', signal), [walkId])
  const { data: fetchedWalk, loading, error, reload } = useWalkResource(`${session?.token ?? ''}:${walkId}`, !!session, request)
  // al llegar desde "Publicar paseo" ya tenemos el detalle y evita el flash de "Cargando…" en el
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

  if (session.user.role === 'CLIENT') {
    const state = location.state as { search?: string; fromBookings?: boolean; bookingsSearch?: string } | null
    return <ClientWalkDetail walk={walk} zone={zone} loading={loading} error={error}
      search={state?.search} fromBookings={state?.fromBookings} bookingsSearch={state?.bookingsSearch} reload={reload} />
  }

  if (ownWalk) return (
    <section className="mx-auto max-w-2xl space-y-5 pb-36" aria-busy={loading}>
      <Link to={paths.myWalks} className="text-label-md text-primary underline-offset-2 hover:underline">Volver a mis paseos</Link>
      {loading && !walk && <p role="status" className="text-body-sm text-on-surface-variant">Cargando el detalle del paseo…</p>}
      {error && (
        <div role="alert" className="rounded-xl bg-error-container p-4">
          <p className="text-body-sm text-on-error-container">{error}</p>
          <button type="button" onClick={reload} className="mt-2 text-label-md font-semibold text-on-error-container underline">Reintentar</button>
        </div>
      )}
      {walk && <>
        <WalkOwnerSummaryCard walk={walk} />
        <div className="flex items-center justify-between px-1">
          <div className="flex items-center gap-2">
            <h2 className="text-title-md font-semibold text-on-surface">Perros de esta salida</h2>
            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary-container text-label-sm text-on-primary-container">
              {walk.bookedSpots}
            </span>
          </div>
          {walk.bookedSpots > 0 && <span className="text-caption text-on-surface-variant">Todos confirmados</span>}
        </div>
        <JoinedPetsRoster walkId={walk.id} />
        <WalkStateActions walkId={walk.id} status={walk.status} bookedSpots={walk.bookedSpots} onChanged={reload} />
      </>}
    </section>
  )

  return (
    <section className="space-y-6" aria-busy={loading}>
      <Link to={ownWalk ? paths.myWalks : paths.walks} className="text-sm font-medium text-blue-700 underline">{ownWalk ? 'Volver a mis paseos' : 'Volver a la búsqueda'}</Link>
      {loading && !walk && <p role="status">Cargando el detalle del paseo…</p>}
      {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-5"><p>{error}</p><button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar detalle</button></div>}
      {walk && <>
        <div className="rounded-2xl border border-blue-100 bg-blue-50 p-6"><p className="text-sm font-semibold text-blue-800">{walkStatusLabels[walk.status]}</p><h1 className="mt-2 text-3xl font-bold">Paseo en {walk.zoneName}</h1><p className="mt-3 break-words text-slate-700">
          Con <Link to={generatePath(paths.walkerProfile, { walkerId: walk.walker.id })} className="font-medium text-blue-700 underline">{walk.walker.name}</Link>
        </p></div>
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
        {walk.status === 'COMPLETED' && (
          <WalkReviewsSection walkId={walk.id} canReview={false} />
        )}
      </>}
    </section>
  )
}
