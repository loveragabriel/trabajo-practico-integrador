import { useCallback } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth'
import { Icon } from '@/shared/components/Icon'
import { isWalkToday } from '../api/search'
import { agendaParams, readAgendaFilters, type AgendaPeriod } from '../api/walkReadContract'
import { loadWalkAgenda } from '../api/walks'
import { WalkAgendaCard } from '../components/WalkAgendaCard'
import { useWalkResource } from '../hooks/useWalkResource'

function greetingWord() {
  const hour = Number(new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Argentina/Buenos_Aires', hour: 'numeric', hourCycle: 'h23',
  }).format(new Date()))
  if (hour < 12) return 'Buen día'
  if (hour < 20) return 'Buenas tardes'
  return 'Buenas noches'
}

function joinNames(names: string[]) {
  if (names.length <= 1) return names[0] ?? ''
  return `${names.slice(0, -1).join(', ')} y ${names[names.length - 1]}`
}

export function MyWalksPage() {
  const { session } = useAuth()
  const [params, setParams] = useSearchParams()
  const query = params.toString()
  const request = useCallback((signal: AbortSignal) => loadWalkAgenda(readAgendaFilters(new URLSearchParams(query)), signal), [query])
  const { data, loading, error, reload } = useWalkResource(`${session?.token ?? ''}:${query}`, !!session, request)
  const period = params.get('period') ?? 'UPCOMING'

  function changePeriod(next: AgendaPeriod) { setParams(agendaParams({ period: next, page: 0 })) }
  function changePage(page: number) {
    setParams(agendaParams({ period: period === 'HISTORY' ? 'HISTORY' : 'UPCOMING', page }))
  }

  // el saludo con "salidas hoy" solo tiene sentido mirando la pestaña de próximas: en
  // historial no tiene relación directa con lo que se está mostrando
  const todaysWalks = period === 'UPCOMING' ? (data?.items.filter((walk) => isWalkToday(walk.startTime)) ?? []) : []
  const todaysZones = Array.from(new Set(todaysWalks.map((walk) => walk.zoneName)))
  // cualquier walk ya cargado trae el rating real del propio paseador (es "mine"): no hace
  // falta un endpoint de perfil aparte, pero si no hay ningún paseo todavía no hay de dónde leerlo
  const rating = data?.items[0]?.walker
  const firstName = session?.user.name.split(' ')[0]

  return (
    <section className="mx-auto max-w-2xl space-y-5 pb-24">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-headline-md font-bold text-primary">{greetingWord()}, {firstName}! 🐾</h1>
        <p className="text-body-sm text-on-surface-variant">
          {period === 'UPCOMING' && data
            ? (todaysWalks.length
              ? <>Tenés <strong className="font-semibold text-primary">{todaysWalks.length} {todaysWalks.length === 1 ? 'salida' : 'salidas'} hoy</strong> en {joinNames(todaysZones)}.</>
              : 'No tenés salidas programadas para hoy.')
            : 'Consultá tus próximas salidas y tu historial de paseos.'}
        </p>
      </div>

      {/* las pestañas van fijas acá, antes de la tarjeta de rating: así no se corren de
          lugar cuando esa tarjeta aparece/desaparece entre pestañas o mientras recarga */}
      <div className="flex gap-1 rounded-full bg-surface-container-low p-1" aria-label="Período de la agenda">
        <button type="button" aria-pressed={period === 'UPCOMING'} onClick={() => changePeriod('UPCOMING')}
          className={`flex-1 rounded-full py-2 text-center text-label-md transition-all ${
            period === 'UPCOMING' ? 'bg-primary-container font-semibold text-on-primary shadow-sm' : 'text-on-surface-variant hover:text-on-surface'
          }`}>
          Próximas salidas
        </button>
        <button type="button" aria-pressed={period === 'HISTORY'} onClick={() => changePeriod('HISTORY')}
          className={`flex-1 rounded-full py-2 text-center text-label-md transition-all ${
            period === 'HISTORY' ? 'bg-primary-container font-semibold text-on-primary shadow-sm' : 'text-on-surface-variant hover:text-on-surface'
          }`}>
          Historial
        </button>
      </div>

      {rating && (
        <div className="flex items-center gap-2.5 rounded-xl bg-surface-container-lowest p-3.5 shadow-sm">
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-secondary-container text-on-secondary-container">
            <Icon name="star" filled className="text-[18px]" />
          </div>
          <div className="flex flex-col">
            <span className="text-label-md font-semibold text-on-surface">{rating.ratingAvg.toLocaleString('es-AR')} / 5</span>
            <span className="text-caption text-on-surface-variant">{rating.reviewCount} reseñas de tus clientes</span>
          </div>
        </div>
      )}

      <div aria-busy={loading} className="space-y-4">
        {loading && <p role="status" className="text-body-sm text-on-surface-variant">Cargando tus paseos…</p>}
        {error && (
          <div role="alert" className="rounded-xl bg-error-container p-4">
            <p className="text-body-sm text-on-error-container">{error}</p>
            <button type="button" onClick={reload} className="mt-2 text-label-md font-semibold text-on-error-container underline">Reintentar agenda</button>
          </div>
        )}
        {data && data.items.length === 0 && (
          <div role="status" className="rounded-2xl bg-surface-container-lowest p-6 shadow-sm">
            <h2 className="text-title-sm font-semibold text-on-surface">
              {period === 'HISTORY' ? 'Todavía no hay paseos en tu historial' : 'No tenés próximas salidas publicadas'}
            </h2>
            <p className="mt-2 text-body-sm text-on-surface-variant">Las salidas que publiques aparecerán acá con su estado y los lugares ocupados.</p>
          </div>
        )}
        {data && data.items.map((walk) => <WalkAgendaCard key={walk.id} walk={walk} />)}
      </div>

      {data && (data.page > 0 || data.hasNext) && (
        <nav aria-label="Páginas de la agenda" className="flex items-center justify-between gap-3">
          <button type="button" disabled={data.page === 0} onClick={() => changePage(data.page - 1)}
            className="rounded-full bg-surface-container-low px-4 py-2 text-label-md font-semibold text-on-surface disabled:opacity-40">
            Anterior
          </button>
          <span className="text-body-sm text-on-surface-variant">Página {data.page + 1}</span>
          <button type="button" disabled={!data.hasNext} onClick={() => changePage(data.page + 1)}
            className="rounded-full bg-surface-container-low px-4 py-2 text-label-md font-semibold text-on-surface disabled:opacity-40">
            Siguiente
          </button>
        </nav>
      )}

      <Link to={paths.publishWalk} aria-label="Publicar nueva salida"
        className="fixed bottom-24 right-5 z-40 flex h-14 w-14 items-center justify-center rounded-full bg-secondary-container text-on-secondary-container shadow-[0_8px_30px_rgba(47,93,80,0.2)] transition-transform hover:scale-105 active:scale-95 md:bottom-8">
        <Icon name="add" className="text-[28px]" />
      </Link>
    </section>
  )
}
