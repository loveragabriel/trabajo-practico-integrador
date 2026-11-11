import { useRef } from 'react'
import { generatePath, Link, useSearchParams } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useZones } from '@/features/map'
import { useProfile } from '@/features/profile/hooks/useProfile'
import { Icon } from '@/shared/components/Icon'
import { formatWalkDate, walkBrowseDays, type WalkSummary } from '../api/search'
import { useWalkSearch } from '../hooks/useWalkSearch'
import { WalkFiltersForm } from './WalkFiltersForm'

export function ClientWalkSearch({ name }: { name: string }) {
  const [params, setParams] = useSearchParams()
  const query = params.toString()
  const { zones, loading: loadingZones, error: zonesError, reload: retryZones } = useZones()
  const { profile } = useProfile()
  const { result, loading, error, reload } = useWalkSearch(query)
  const days = walkBrowseDays()
  const homeZone = zones.find((zone) => zone.id === profile?.zoneId)
  const zoneId = params.get('zoneId') ?? ''
  const zoneSelect = useRef<HTMLSelectElement>(null)
  const isAllDays = !params.has('startsFrom') && !params.has('startsTo')

  function changeFilters(values: Record<string, string>) {
    const next = new URLSearchParams(params)
    next.delete('page')
    for (const [key, value] of Object.entries(values)) {
      if (value) next.set(key, value)
      else next.delete(key)
    }
    setParams(next)
  }

  return (
    <section className="mx-auto max-w-2xl space-y-5">
      <header className="flex min-w-0 items-center gap-3">
        <span className="grid size-13 shrink-0 place-items-center rounded-full bg-primary-fixed text-primary"><Icon name="pets" filled className="text-[27px]" /></span>
        <div className="min-w-0 flex-1">
          <h1 className="break-words text-headline-md font-bold text-on-surface">¡Hola, {name.split(/\s+/)[0]}!</h1>
          <p className="text-body-sm text-on-surface-variant">Encontrá paseos para tu mascota.</p>
        </div>
      </header>

      <div className="flex items-center gap-2 rounded-full bg-surface-container-lowest px-4 shadow-sm">
        <Icon name="location_on" filled className="shrink-0 text-xl text-primary" />
        <select ref={zoneSelect} aria-label="Barrio" value={zoneId} onChange={(e) => changeFilters({ zoneId: e.target.value })} disabled={loadingZones}
          className="min-h-12 min-w-0 flex-1 bg-transparent text-label-md font-semibold text-primary">
          <option value="">{homeZone ? `${homeZone.name} · Mi barrio` : 'Mi barrio'}</option>
          {zoneId && !zones.some((zone) => zone.id === zoneId) && <option value={zoneId}>Barrio no disponible</option>}
          {zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name}</option>)}
        </select>
      </div>
      {zonesError && <p role="alert" className="text-body-sm text-on-error-container">{zonesError} <button type="button" onClick={retryZones} className="font-semibold underline">Reintentar barrios</button></p>}

      <div role="group" aria-label="Día del paseo" className="flex gap-2 overflow-x-auto px-0.5 py-1">
        <button type="button" aria-pressed={isAllDays} onClick={() => changeFilters({ startsFrom: '', startsTo: '' })}
          className={`shrink-0 rounded-2xl px-4 text-label-md font-semibold shadow-sm ${isAllDays ? 'bg-primary-container text-on-primary' : 'bg-surface-container-lowest text-on-surface-variant'}`}>Todos</button>
        {days.map((day) => {
          const active = [day.from, `${day.from}:00`].includes(params.get('startsFrom') ?? '')
            && [day.to, `${day.to}:00`].includes(params.get('startsTo') ?? '')
          return (
            <button key={day.from} type="button" aria-pressed={active}
              onClick={() => changeFilters({ startsFrom: day.from, startsTo: day.to })}
              className={`flex min-h-14 min-w-13 shrink-0 flex-col items-center justify-center rounded-2xl px-2 text-label-sm font-semibold capitalize shadow-sm ${active ? 'bg-primary-container text-on-primary' : 'bg-surface-container-lowest text-on-surface-variant'}`}>
              {day.label}<span className="text-title-sm">{day.date}</span>
            </button>
          )
        })}
      </div>

      <section className="space-y-3" aria-busy={loading}>
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-title-md font-bold text-on-surface">Salidas en grupo</h2>
          <button type="button" popoverTarget="client-search-filters" className="flex min-h-10 items-center gap-1 rounded-full bg-surface-container-low px-3 text-label-md font-semibold text-primary">
            <Icon name="tune" className="text-lg" /> Filtros
          </button>
        </div>
        <div id="client-search-filters" popover="auto" className="fixed inset-x-0 top-1/2 mx-auto max-h-[80dvh] w-[calc(100%_-_2.5rem)] max-w-lg -translate-y-1/2 overflow-auto rounded-2xl bg-surface-container-lowest p-4 shadow-xl backdrop:bg-black/25">
          <div className="mb-2 flex items-center justify-between px-1">
            <h3 className="text-title-md font-semibold text-primary">Filtrá por horario</h3>
            <button type="button" popoverTarget="client-search-filters" popoverTargetAction="hide" aria-label="Cerrar filtros" className="grid size-10 place-items-center rounded-full bg-surface-container-low text-primary"><Icon name="close" className="text-lg" /></button>
          </div>
          <WalkFiltersForm key={query} params={params} zones={zones} loadingZones={loadingZones} onSearch={(next) => {
            setParams(next)
            document.getElementById('client-search-filters')?.hidePopover()
          }} client />
        </div>
        {loading && <p role="status" className="rounded-2xl bg-surface-container-lowest p-5 text-body-sm text-on-surface-variant">Buscando paseos…</p>}
        {error && (
          <div role="alert" className="rounded-2xl bg-error-container p-4 text-on-error-container">
            <p>{error}</p>
            <button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar búsqueda</button>
          </div>
        )}
        {result?.items.length === 0 && (
          <div className="rounded-2xl bg-surface-container-lowest p-6 text-center shadow-sm">
            <span className="mx-auto grid size-16 place-items-center rounded-full bg-primary-fixed/60 text-primary"><Icon name="search_off" className="text-3xl" /></span>
            <h3 className="mt-3 text-title-md font-semibold text-on-surface">Sin paseos disponibles en tu zona</h3>
            <p className="mt-2 text-body-sm text-on-surface-variant">Probá otro día o elegí un barrio cercano para encontrar más salidas.</p>
            <button type="button"
              onClick={() => changeFilters(params.get('startsFrom')?.startsWith(days[1].from) ? { startsFrom: '', startsTo: '' } : { startsFrom: days[1].from, startsTo: days[1].to })}
              className="mt-4 flex h-[52px] w-full items-center justify-center gap-2 rounded-full bg-primary-container px-4 text-label-md font-semibold text-on-primary">
              <Icon name="event" className="text-xl" />
              {params.get('startsFrom')?.startsWith(days[1].from) ? 'Ver todos los días' : 'Ver paseos para mañana'}
            </button>
            <button type="button" onClick={() => {
              zoneSelect.current?.focus()
              try { zoneSelect.current?.showPicker?.() } catch { /* El selector conserva el foco si el navegador no abre el menú. */ }
            }}
              className="mt-2 min-h-11 px-3 text-label-md font-semibold text-primary">Cambiar de barrio</button>
          </div>
        )}
        {result && result.items.map((walk) => <ClientWalkCard key={walk.id} walk={walk} search={query} />)}
        {result && (result.page > 0 || result.hasNext) && (
          <nav aria-label="Páginas de resultados" className="flex items-center justify-between text-label-md">
            <button type="button" disabled={!result.page} onClick={() => setParams((previous) => { const next = new URLSearchParams(previous); next.set('page', String(result.page - 1)); return next })}
              className="rounded-full bg-surface-container-low px-4 py-2 text-primary disabled:opacity-40">Anterior</button>
            <span>Página {result.page + 1}</span>
            <button type="button" disabled={!result.hasNext} onClick={() => setParams((previous) => { const next = new URLSearchParams(previous); next.set('page', String(result.page + 1)); return next })}
              className="rounded-full bg-surface-container-low px-4 py-2 text-primary disabled:opacity-40">Siguiente</button>
          </nav>
        )}
      </section>
    </section>
  )
}

function ClientWalkCard({ walk, search }: { walk: WalkSummary; search: string }) {
  return (
    <article>
      <Link to={generatePath(paths.walkDetail, { walkId: walk.id })} state={{ search }} className="block rounded-2xl bg-surface-container-lowest p-4 shadow-sm outline-offset-2 transition-shadow hover:shadow-md focus-visible:outline-primary">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className="text-label-sm font-semibold text-primary">{walk.zoneName} · Paseo grupal</span>
          <span className={`rounded-full px-2.5 py-1 text-label-sm font-semibold ${walk.availableSpots <= 2 ? 'bg-secondary-fixed text-on-secondary-fixed-variant' : 'bg-primary-fixed text-on-primary-fixed-variant'}`}>
            {walk.availableSpots} {walk.availableSpots === 1 ? 'cupo libre' : 'cupos libres'}
          </span>
        </div>
        <div className="mt-3 flex items-center gap-3">
          <span className="grid size-10 shrink-0 place-items-center rounded-full bg-primary-fixed/60 text-primary"><Icon name="person" className="text-xl" /></span>
          <div className="min-w-0 flex-1">
            <h3 className="break-words text-title-sm font-semibold text-on-surface">{walk.walker.name}</h3>
            <p className="text-caption text-on-surface-variant">{walk.walker.reviewCount ? `★ ${walk.walker.ratingAvg.toLocaleString('es-AR')} · ${walk.walker.reviewCount} valoraciones` : 'Todavía sin reseñas'}</p>
          </div>
          <Icon name="chevron_right" className="shrink-0 text-2xl text-primary" />
        </div>
        <div className="mt-3 space-y-1 text-body-sm text-on-surface-variant">
          <p className="flex gap-2"><Icon name="schedule" className="shrink-0 text-lg text-primary" /><span>{formatWalkDate(walk.startTime)} · {walk.durationMinutes} min</span></p>
          <p className="flex gap-2"><Icon name="location_on" className="shrink-0 text-lg text-primary" /><span className="min-w-0 break-words">{walk.meetingPoint}</span></p>
        </div>
        <p className="mt-3 border-t border-outline-variant/30 pt-3 text-title-sm font-bold text-primary">
          {walk.pricePerPet.toLocaleString('es-AR', { style: 'currency', currency: 'ARS', minimumFractionDigits: 0 })} <span className="text-caption font-normal text-on-surface-variant">por mascota</span>
        </p>
      </Link>
    </article>
  )
}
