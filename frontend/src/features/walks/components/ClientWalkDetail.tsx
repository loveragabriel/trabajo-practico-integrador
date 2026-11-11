import { useEffect, useRef, useState, type FormEvent } from 'react'
import { generatePath, Link, useLocation, useNavigate } from 'react-router-dom'
import { paths } from '@/app/paths'
import { bookingError, joinWalk } from '@/features/bookings/api/bookings'
import { ZoneCoverageMap, type Zone } from '@/features/map'
import { petSizeLabels } from '@/features/pets/api/pets'
import { usePets } from '@/features/pets/hooks/usePets'
import { WalkReviewsSection } from '@/features/reviews/components/WalkReviewsSection'
import { Icon } from '@/shared/components/Icon'
import { formatWalkDate, walkTimeOnly } from '../api/search'
import { walkStatusBadgeClass, walkStatusLabels, type WalkDetails } from '../api/walkReadContract'

interface Props {
  walk?: WalkDetails
  zone?: Zone
  loading: boolean
  error?: string
  search?: string
  fromBookings?: boolean
  bookingsSearch?: string
  reload: () => void
}

export function ClientWalkDetail({ walk, zone, loading, error, search, fromBookings, bookingsSearch, reload }: Props) {
  const { hash } = useLocation()
  const reviews = useRef<HTMLElement>(null)
  const completed = walk?.status === 'COMPLETED'
  const back = fromBookings ? { pathname: paths.bookings, search: bookingsSearch } : { pathname: paths.walks, search }
  useEffect(() => {
    if (completed && hash === '#resenas') reviews.current?.scrollIntoView()
  }, [completed, hash])
  return (
    <section className="mx-auto max-w-2xl space-y-4" aria-busy={loading}>
      <Link to={back} className="inline-flex min-h-10 items-center gap-1 text-label-md font-semibold text-primary">
        <Icon name="arrow_back" className="text-lg" /> {fromBookings ? 'Mis reservas' : 'Volver a la búsqueda'}
      </Link>
      {loading && !walk && <p role="status" className="text-body-sm text-on-surface-variant">Cargando el detalle del paseo…</p>}
      {error && (
        <div role="alert" className="rounded-2xl bg-error-container p-4 text-on-error-container">
          <p>{error}</p>
          <button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar detalle</button>
        </div>
      )}
      {walk && (
        <>
          <header className="relative overflow-hidden rounded-2xl bg-primary-fixed/45">
            {zone ? <ZoneCoverageMap zones={[zone]} className="h-36" /> : <div className="flex h-28 items-center justify-center text-primary"><Icon name="map" className="text-4xl" /></div>}
            <p className="flex items-center gap-2 px-4 py-2 text-caption font-medium text-primary"><Icon name="location_on" className="text-lg" /> Mapa orientativo de {walk.zoneName}</p>
          </header>
          <div className="rounded-2xl bg-surface-container-lowest p-4 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h1 className="text-headline-md font-bold text-on-surface">Paseo en {walk.zoneName}</h1>
              <span className={`rounded-full px-3 py-1 text-label-sm font-semibold ${walkStatusBadgeClass[walk.status]}`}>{walkStatusLabels[walk.status]}</span>
            </div>
            <div className="mt-4 flex items-center gap-3">
              <span className="grid size-11 shrink-0 place-items-center rounded-full bg-primary-fixed text-primary"><Icon name="person" className="text-xl" /></span>
              <div className="min-w-0 flex-1">
                <p className="break-words font-semibold text-on-surface">{walk.walker.name}</p>
                <p className="text-caption text-on-surface-variant">{walk.walker.reviewCount ? `★ ${walk.walker.ratingAvg.toLocaleString('es-AR')} · ${walk.walker.reviewCount} valoraciones` : 'Todavía sin reseñas'}</p>
              </div>
              <Link to={generatePath(paths.walkerProfile, { walkerId: walk.walker.id })} className="shrink-0 rounded-full bg-surface-container-low px-3 py-2 text-label-sm font-semibold text-primary">Ver perfil</Link>
            </div>
          </div>

          <section className="rounded-2xl bg-surface-container-lowest p-4 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="flex items-center gap-2 text-label-lg font-semibold text-primary"><Icon name="groups" className="text-xl" /> Ocupación: {walk.bookedSpots} de {walk.capacity}</h2>
              <span className="rounded-full bg-secondary-fixed px-2.5 py-1 text-label-sm font-semibold text-on-secondary-fixed-variant">{walk.availableSpots ? `${walk.availableSpots} ${walk.availableSpots === 1 ? 'cupo libre' : 'cupos libres'}` : 'Sin cupos'}</span>
            </div>
            <progress aria-label="Lugares ocupados" value={walk.bookedSpots} max={walk.capacity} className="mt-3 block h-2 w-full appearance-none overflow-hidden rounded-full bg-surface-container-high [&::-moz-progress-bar]:rounded-full [&::-moz-progress-bar]:bg-primary-container [&::-webkit-progress-bar]:bg-surface-container-high [&::-webkit-progress-value]:rounded-full [&::-webkit-progress-value]:bg-primary-container" />
          </section>

          <section className="rounded-2xl bg-surface-container-lowest p-4 shadow-sm">
            <h2 className="mb-3 text-title-md font-semibold text-on-surface">Detalles del paseo</h2>
            <dl className="space-y-4 text-body-sm text-on-surface-variant">
              <div className="flex gap-3">
                <Icon name="calendar_today" className="shrink-0 text-xl text-primary" />
                <div><dt className="font-semibold text-on-surface">Día y horario</dt><dd>{formatWalkDate(walk.startTime)} a {walkTimeOnly(walk.endTime)} · {walk.durationMinutes} min</dd></div>
              </div>
              <div className="flex gap-3">
                <Icon name="location_on" className="shrink-0 text-xl text-primary" />
                <div className="min-w-0"><dt className="font-semibold text-on-surface">Punto de encuentro</dt><dd className="break-words">{walk.meetingPoint}</dd></div>
              </div>
              <div className="flex gap-3">
                <Icon name="payments" className="shrink-0 text-xl text-primary" />
                <div><dt className="font-semibold text-on-surface">Tarifa por mascota</dt><dd>{formatPrice(walk.pricePerPet)}</dd></div>
              </div>
            </dl>
          </section>

          {walk.status === 'PUBLISHED' && walk.availableSpots > 0
            ? <PetReservationForm key={walk.id} walk={walk} />
            : <p className="rounded-2xl bg-surface-container-low p-4 text-body-sm text-on-surface-variant">
                {walk.status === 'FULL' ? 'Este paseo ya no tiene lugares disponibles.' : 'Este paseo ya no admite inscripciones.'}
              </p>}
          {completed && (
            <section id="resenas" ref={reviews} className="scroll-mt-24">
              <WalkReviewsSection walkId={walk.id} canReview />
            </section>
          )}
        </>
      )}
    </section>
  )
}

function formatPrice(value: number) {
  return value.toLocaleString('es-AR', { style: 'currency', currency: 'ARS', minimumFractionDigits: 0 })
}

function PetReservationForm({ walk }: { walk: WalkDetails }) {
  const { pets, error: petsError, loading, reload } = usePets()
  const [selectedId, select] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const posting = useRef(false)
  const navigate = useNavigate()
  const selected = pets?.find((pet) => pet.id === selectedId) ?? (pets?.length === 1 ? pets[0] : undefined)
  const addPet = <Link to={paths.newPet} state={{ walkId: walk.id }} className="inline-flex min-h-11 items-center gap-1 text-label-md font-semibold text-primary">
    <Icon name="add" className="text-lg" /> Agregar mascota
  </Link>

  async function reserve(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selected || posting.current) return
    posting.current = true
    setSaving(true)
    setError('')
    try {
      await joinWalk(walk.id, selected.id)
      navigate(paths.bookings, { state: { confirmedPet: selected.name } })
    } catch (failure) {
      setError(bookingError(failure))
    } finally {
      posting.current = false
      setSaving(false)
    }
  }

  if (loading) return <p role="status" className="text-body-sm text-on-surface-variant">Cargando tus mascotas…</p>
  if (petsError) return (
    <div role="alert" className="rounded-2xl bg-error-container p-4 text-on-error-container">
      <p>{petsError}</p><button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar mascotas</button>
    </div>
  )
  if (!pets?.length) return (
    <div className="rounded-2xl bg-surface-container-lowest p-4 shadow-sm">
      <p>Necesitás una mascota registrada para reservar.</p>
      {addPet}
    </div>
  )

  return (
    <form onSubmit={reserve} className="pb-26 md:pb-0">
      <fieldset disabled={saving} className="space-y-2">
        <legend className="mb-2 text-title-md font-semibold text-on-surface">Elegí tu mascota para esta salida</legend>
        <div className="flex gap-3 overflow-x-auto p-1">
          {pets.map((pet) => (
            <label key={pet.id} className="relative w-33 shrink-0 cursor-pointer">
              <input type="radio" name="pet" value={pet.id} checked={selected?.id === pet.id} onChange={() => select(pet.id)} className="peer sr-only" />
              <span className="flex min-h-26 flex-col items-center gap-1 rounded-2xl bg-surface-container-lowest p-3 text-center shadow-sm peer-checked:bg-primary-fixed/55 peer-checked:ring-2 peer-checked:ring-primary peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-primary">
                <Icon name={selected?.id === pet.id ? 'check_circle' : 'pets'} filled className="text-2xl text-primary" />
                <span className="max-w-full break-words text-label-md font-semibold">{pet.name}</span>
                <span className="max-w-full break-words text-caption text-on-surface-variant">{pet.size ? petSizeLabels[pet.size] : pet.breed || 'Mascota'}</span>
              </span>
            </label>
          ))}
        </div>
        {addPet}
      </fieldset>
      <div className="fixed inset-x-0 bottom-[calc(4rem+env(safe-area-inset-bottom,0px))] z-40 border-t border-outline-variant/40 bg-surface-container-lowest px-5 py-3 shadow-lg md:static md:mt-3 md:rounded-2xl md:border-0 md:shadow-sm">
        <div className="mx-auto max-w-2xl">
          {error && <p role="alert" className="mb-2 rounded-xl bg-error-container p-2 text-body-sm text-on-error-container">{error}</p>}
          <div className="flex items-center gap-3">
            <div className="shrink-0">
              <p className="text-caption text-on-surface-variant">Por mascota</p>
              <p className="font-bold text-primary">{formatPrice(walk.pricePerPet)}</p>
            </div>
            <button type="submit" disabled={!selected || saving} className="flex min-h-[52px] min-w-0 flex-1 items-center justify-center gap-1.5 rounded-full bg-primary-container px-3 py-2 text-label-md font-semibold text-on-primary disabled:opacity-55">
              <Icon name="pets" className="shrink-0 text-xl" />
              <span className="min-w-0 break-words">{saving ? 'Reservando…' : selected ? `Sumar a ${selected.name}` : 'Elegí tu mascota'}</span>
            </button>
          </div>
        </div>
      </div>
    </form>
  )
}
