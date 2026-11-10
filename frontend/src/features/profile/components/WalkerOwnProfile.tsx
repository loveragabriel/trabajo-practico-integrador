import { useEffect, useId, useState, type SubmitEvent } from 'react'
import { useAuth } from '@/features/auth'
import { useZones, ZoneCoverageMap, type Zone } from '@/features/map'
import { ApiRequestError } from '@/shared/api/client'
import { Icon } from '@/shared/components/Icon'
import { ROLE_LABELS } from '@/shared/types/user'
import { updateCoverage, type Coverage } from '../api/coverage'
import { loadMyReviews, type ReceivedReview } from '../api/myReviews'
import { updateMyProfile } from '../api/profile'
import { useCoverage } from '../hooks/useCoverage'
import type { UserProfile } from '../types'

const MIN_CAPACITY = 1
// tope solo de UI, igual que en Publicar paseo: RN-15 no impone un máximo
const MAX_CAPACITY = 30

function initials(name: string) {
  return name.trim().slice(0, 1).toUpperCase() || '?'
}

function memberSince(createdAt: string) {
  return new Intl.DateTimeFormat('es-AR', { month: 'long', year: 'numeric' }).format(new Date(createdAt))
}

function formatReviewDate(value: string) {
  const days = Math.floor((Date.now() - new Date(value).getTime()) / (1000 * 60 * 60 * 24))
  if (days <= 0) return 'Hoy'
  if (days === 1) return 'Ayer'
  if (days < 7) return `Hace ${days} días`
  if (days < 30) return `Hace ${Math.floor(days / 7)} sem.`
  return new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(value))
}

function ReviewsCard({ ratingAvg, reviewCount }: { ratingAvg: number; reviewCount: number }) {
  const [reviews, setReviews] = useState<ReceivedReview[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    loadMyReviews(controller.signal).then(setReviews).catch((failure: unknown) => {
      if (controller.signal.aborted) return
      setError(failure instanceof Error ? failure.message : 'No pudimos cargar tus reseñas.')
    })
    return () => controller.abort()
  }, [])

  return (
    <section className="flex flex-col gap-4 rounded-[20px] bg-surface-container-lowest p-5 shadow-sm">
      <div className="flex items-center gap-2">
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-secondary-container/20 text-secondary">
          <Icon name="star" filled className="text-[18px]" />
        </span>
        <div>
          <h2 className="text-title-sm font-bold text-primary">Reseñas recientes</h2>
          <p className="text-caption text-on-surface-variant">
            {reviewCount > 0 ? `Calificación promedio: ${ratingAvg.toLocaleString('es-AR')} ★ (${reviewCount})` : 'Todavía sin reseñas'}
          </p>
        </div>
      </div>

      {error && <p role="alert" className="text-body-sm text-error">{error}</p>}
      {!error && !reviews && <p role="status" className="text-body-sm text-on-surface-variant">Cargando reseñas…</p>}
      {reviews && reviews.length === 0 && <p className="text-body-sm text-on-surface-variant">Todavía no recibiste reseñas.</p>}
      {reviews && reviews.length > 0 && (
        <div className="flex flex-col gap-3">
          {reviews.slice(0, 5).map((review) => (
            <article key={review.id} className="flex flex-col gap-2 rounded-xl bg-surface-container-low p-4">
              <div className="flex items-center justify-between gap-2">
                <span className="truncate text-label-md font-semibold text-on-surface">{review.authorName}</span>
                <span className="shrink-0 text-caption text-outline">{formatReviewDate(review.createdAt)}</span>
              </div>
              <div className="flex items-center gap-0.5 text-secondary">
                {Array.from({ length: 5 }, (_, index) => (
                  <Icon key={index} name="star" filled={index < review.rating} className="text-[16px]" />
                ))}
              </div>
              {review.comment && <p className="text-body-sm italic leading-snug text-on-surface">“{review.comment}”</p>}
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

interface FormProps {
  profile: UserProfile
  coverage: Coverage
  zones: Zone[]
  onSaved: (profile: UserProfile) => void
  onSignOut: () => void
}

function WalkerProfileForm({ profile, coverage, zones, onSaved, onSignOut }: FormProps) {
  const fullNameId = useId()
  const phoneId = useId()
  const homeZoneFieldId = useId()
  const priceId = useId()

  const [fullName, setFullName] = useState(profile.fullName)
  const [phone, setPhone] = useState(profile.phone ?? '')
  const [homeZoneId, setHomeZoneId] = useState(profile.zoneId)
  const [price, setPrice] = useState(coverage.defaultPrice != null ? String(coverage.defaultPrice) : '')
  const [capacity, setCapacity] = useState(coverage.defaultCapacity ?? MIN_CAPACITY)
  const [selectedZones, setSelectedZones] = useState<string[]>(() => coverage.zones.map((zone) => zone.id))
  const [zoneSearch, setZoneSearch] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const normalizedSearch = zoneSearch.trim().toLowerCase()
  const filteredZones = normalizedSearch
    ? zones.filter((zone) => zone.name.toLowerCase().includes(normalizedSearch) || zone.city.toLowerCase().includes(normalizedSearch))
    : zones

  function toggleZone(zoneId: string, checked: boolean) {
    setSuccess(false)
    setSelectedZones((current) => (checked ? [...current, zoneId] : current.filter((id) => id !== zoneId)))
  }

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting) return
    if (selectedZones.length === 0) {
      setError('Elegí al menos un barrio de cobertura.')
      return
    }
    setSubmitting(true)
    setError(null)
    setSuccess(false)
    try {
      // dos operaciones reales (datos personales + parámetros de paseador) detrás de un solo
      // botón: el formulario del mockup las muestra juntas, pero son dos PUT distintos
      const updatedProfile = await updateMyProfile({ fullName, phone: phone || undefined, zoneId: homeZoneId })
      await updateCoverage({
        defaultPrice: price === '' ? null : Number(price),
        defaultCapacity: capacity,
        zoneIds: selectedZones,
      })
      onSaved(updatedProfile)
      setSuccess(true)
    } catch (failure) {
      setError(
        failure instanceof ApiRequestError
          ? (failure.fieldErrors?.[0]?.message ?? failure.message)
          : 'No pudimos guardar los cambios.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mx-auto flex max-w-2xl flex-col gap-5">
      <fieldset disabled={submitting} className="flex flex-col gap-5">
        {/* Resumen de perfil */}
        <section className="flex flex-col gap-4 rounded-[20px] bg-surface-container-lowest p-5 shadow-sm">
          <div className="flex items-center gap-3.5">
            <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-primary-container text-headline-sm font-bold text-on-primary ring-4 ring-primary-fixed/30">
              {initials(fullName)}
            </div>
            <div className="flex min-w-0 flex-col">
              <h1 className="truncate text-headline-sm font-bold text-primary">{fullName}</h1>
              <p className="mt-0.5 flex items-center gap-1 text-caption text-on-surface-variant">
                <Icon name="calendar_month" className="text-[14px]" />
                Miembro desde {memberSince(profile.createdAt)}
              </p>
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <label htmlFor={fullNameId} className="flex flex-col gap-1.5">
              <span className="text-label-md font-semibold text-on-surface">Nombre completo</span>
              <input id={fullNameId} value={fullName} onChange={(event) => setFullName(event.target.value)} required
                autoComplete="name"
                className="h-[46px] rounded-lg bg-surface-container-low px-3.5 text-body-md text-on-surface outline-none focus:bg-surface-container-lowest focus:shadow-[0_0_0_2px_var(--color-primary)]" />
            </label>
            <label htmlFor={phoneId} className="flex flex-col gap-1.5">
              <span className="text-label-md font-semibold text-on-surface">Teléfono</span>
              <input id={phoneId} type="tel" value={phone} onChange={(event) => setPhone(event.target.value)}
                autoComplete="tel" placeholder="+54 9 11…"
                className="h-[46px] rounded-lg bg-surface-container-low px-3.5 text-body-md text-on-surface outline-none focus:bg-surface-container-lowest focus:shadow-[0_0_0_2px_var(--color-primary)]" />
            </label>
          </div>

          <label htmlFor={homeZoneFieldId} className="flex flex-col gap-1.5">
            <span className="text-label-md font-semibold text-on-surface">Vivís en</span>
            <select id={homeZoneFieldId} value={homeZoneId} onChange={(event) => setHomeZoneId(event.target.value)} required
              className="h-[46px] rounded-lg bg-surface-container-low px-3.5 text-body-md text-on-surface outline-none focus:bg-surface-container-lowest focus:shadow-[0_0_0_2px_var(--color-primary)]">
              {zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name} — {zone.city}</option>)}
            </select>
          </label>

          <div className="grid grid-cols-2 gap-2 rounded-xl bg-surface-container-low/60 p-3 text-center">
            <div className="flex flex-col items-center">
              <div className="flex items-center gap-0.5 text-title-sm font-bold text-secondary">
                <span>{profile.reviewCount > 0 ? profile.ratingAvg.toLocaleString('es-AR') : '—'}</span>
                <Icon name="star" filled className="text-[16px]" />
              </div>
              <span className="mt-0.5 text-label-sm text-on-surface-variant">{profile.reviewCount} reseñas</span>
            </div>
            <div className="flex flex-col items-center">
              <span className="text-title-sm font-bold text-primary">{ROLE_LABELS[profile.role]}</span>
              <span className="mt-0.5 text-label-sm text-on-surface-variant">Tipo de cuenta</span>
            </div>
          </div>
        </section>

        {/* Parámetros por defecto */}
        <section className="flex flex-col gap-4 rounded-[20px] bg-surface-container-lowest p-5 shadow-sm">
          <div className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-fixed/40 text-primary">
              <Icon name="tune" className="text-[18px]" />
            </span>
            <div>
              <h2 className="text-title-sm font-bold text-primary">Parámetros por defecto</h2>
              <p className="text-caption text-on-surface-variant">Precargan el formulario al publicar una salida</p>
            </div>
          </div>

          <div className="flex items-center justify-between rounded-xl bg-surface-container-low p-3.5">
            <div className="flex flex-col">
              <label htmlFor={priceId} className="text-label-md text-on-surface">Tarifa por perro</label>
              <span className="text-caption text-on-surface-variant">Por salida individual o en manada</span>
            </div>
            <div className="flex items-center gap-1.5 rounded-lg bg-surface-container-lowest px-3 py-1.5 shadow-sm">
              <span className="text-title-sm font-bold text-primary">$</span>
              <input id={priceId} type="number" min={0} max={99999999.99} step={0.01} value={price}
                onChange={(event) => setPrice(event.target.value)}
                className="w-24 bg-transparent text-title-sm font-bold text-primary outline-none" />
              <span className="text-label-sm text-on-surface-variant">ARS</span>
            </div>
          </div>

          <div className="flex items-center justify-between rounded-xl bg-surface-container-low p-3.5">
            <div className="flex flex-col">
              <span className="text-label-md text-on-surface">Cupo máximo estándar</span>
              <span className="text-caption text-on-surface-variant">Recomendado para control óptimo</span>
            </div>
            <div className="flex items-center gap-2 rounded-full bg-surface-container-lowest p-1 shadow-sm">
              <button type="button" aria-label="Disminuir cupo" disabled={capacity <= MIN_CAPACITY}
                onClick={() => setCapacity((value) => Math.max(MIN_CAPACITY, value - 1))}
                className="flex h-7 w-7 items-center justify-center rounded-full bg-surface-container-low text-on-surface transition-transform active:scale-90 disabled:opacity-40">
                <Icon name="remove" className="text-[16px]" />
              </button>
              <span className="w-6 text-center text-title-sm font-bold text-primary">{capacity}</span>
              <button type="button" aria-label="Aumentar cupo" disabled={capacity >= MAX_CAPACITY}
                onClick={() => setCapacity((value) => Math.min(MAX_CAPACITY, value + 1))}
                className="flex h-7 w-7 items-center justify-center rounded-full bg-primary text-on-primary transition-transform active:scale-90 disabled:opacity-40">
                <Icon name="add" className="text-[16px]" />
              </button>
            </div>
          </div>
        </section>

        {/* Zonas de cobertura */}
        <section className="flex flex-col gap-4 rounded-[20px] bg-surface-container-lowest p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="flex h-8 w-8 items-center justify-center rounded-full bg-secondary-container/30 text-secondary">
                <Icon name="map" className="text-[18px]" />
              </span>
              <div>
                <h2 className="text-title-sm font-bold text-primary">Zonas de cobertura</h2>
                <p className="text-caption text-on-surface-variant">Barrios donde recibís reservas</p>
              </div>
            </div>
            <span className="rounded-full bg-primary-fixed/50 px-2 py-0.5 text-label-sm font-semibold text-primary">
              {selectedZones.length} {selectedZones.length === 1 ? 'activa' : 'activas'}
            </span>
          </div>

          {selectedZones.length > 0 && (
            <ZoneCoverageMap zones={zones.filter((zone) => selectedZones.includes(zone.id))} className="h-40" />
          )}

          <div className="relative">
            <Icon name="search" className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-[18px] text-on-surface-variant" />
            <input type="text" value={zoneSearch} onChange={(event) => setZoneSearch(event.target.value)}
              placeholder="Buscar barrio…" aria-label="Buscar barrio"
              className="h-[42px] w-full rounded-lg bg-surface-container-low pl-10 pr-3.5 text-body-sm text-on-surface outline-none placeholder:text-outline focus:bg-surface-container-lowest focus:shadow-[0_0_0_2px_var(--color-primary)]" />
          </div>

          {/* catálogo con ~50 barrios: el buscador filtra y el scroll acotado evita que la
              tarjeta crezca sin límite hacia abajo. min-h-0 es necesario: sin eso, un hijo
              flex ignora max-height/overflow y se estira a lo que mida su contenido */}
          <div className="flex min-h-0 max-h-72 flex-col gap-2 overflow-y-auto pr-1">
            {filteredZones.map((zone) => {
              const checked = selectedZones.includes(zone.id)
              return (
                <label key={zone.id}
                  className={`flex cursor-pointer items-center justify-between rounded-xl p-3 transition-colors hover:bg-surface-container/70 ${checked ? 'bg-surface-container-low' : 'bg-surface-container-low/60'}`}>
                  <div className="flex min-w-0 items-center gap-3">
                    <div className={`flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-md ${checked ? 'bg-primary-container text-on-primary' : 'bg-surface-container-highest'}`}>
                      {checked && <Icon name="check" className="text-[16px]" />}
                    </div>
                    <div className="flex min-w-0 flex-col">
                      <span className="truncate text-label-md text-on-surface">{zone.name}</span>
                      <span className="truncate text-caption text-on-surface-variant">{zone.city}</span>
                    </div>
                  </div>
                  <input type="checkbox" className="sr-only" checked={checked}
                    onChange={(event) => toggleZone(zone.id, event.target.checked)} />
                  <span className={`shrink-0 rounded-full px-2 py-0.5 text-label-sm font-semibold ${checked ? 'bg-primary-fixed text-on-primary-fixed' : 'text-outline'}`}>
                    {checked ? 'Activa' : 'Agregar'}
                  </span>
                </label>
              )
            })}
            {filteredZones.length === 0 && <p className="p-3 text-body-sm text-on-surface-variant">No encontramos barrios para "{zoneSearch}".</p>}
          </div>
          {selectedZones.length === 0 && <p className="text-body-sm text-error">Elegí al menos un barrio de cobertura.</p>}
        </section>
      </fieldset>

      <ReviewsCard ratingAvg={profile.ratingAvg} reviewCount={profile.reviewCount} />

      {error && <p role="alert" className="rounded-xl bg-error-container p-3 text-body-sm text-on-error-container">{error}</p>}
      {success && <p role="status" className="text-body-sm font-medium text-primary">Cambios guardados.</p>}

      <div className="flex flex-col gap-3">
        <button type="submit" disabled={submitting}
          className="flex h-[52px] w-full items-center justify-center gap-2 rounded-full bg-primary-container text-label-lg font-semibold text-on-primary shadow-md transition-all active:scale-[0.98] disabled:opacity-60">
          <Icon name="check" className="text-[20px]" />
          {submitting ? 'Guardando…' : 'Guardar cambios del perfil'}
        </button>
        <button type="button" onClick={onSignOut}
          className="flex w-full items-center justify-center gap-1.5 rounded-full py-3 text-label-md text-tertiary transition-colors hover:bg-error-container/40">
          <Icon name="logout" className="text-[18px]" />
          Cerrar sesión
        </button>
      </div>
    </form>
  )
}

export function WalkerOwnProfile({ profile, onSaved }: { profile: UserProfile; onSaved: (profile: UserProfile) => void }) {
  const { session, signOut } = useAuth()
  const { coverage, loading: coverageLoading, error: coverageError, reload: reloadCoverage } = useCoverage()
  const catalog = useZones(session?.token ?? null)

  if (coverageError || catalog.error) {
    return (
      <div role="alert" className="mx-auto max-w-2xl rounded-xl bg-error-container p-4 text-body-sm text-on-error-container">
        <p>{coverageError || catalog.error}</p>
        <button type="button" onClick={() => { reloadCoverage(); catalog.reload() }} className="mt-2 font-semibold underline">Reintentar</button>
      </div>
    )
  }
  if (coverageLoading || catalog.loading || !coverage) {
    return <p role="status" className="mx-auto max-w-2xl text-body-sm text-on-surface-variant">Cargando tu perfil de paseador…</p>
  }

  return <WalkerProfileForm profile={profile} coverage={coverage} zones={catalog.zones} onSaved={onSaved} onSignOut={signOut} />
}
