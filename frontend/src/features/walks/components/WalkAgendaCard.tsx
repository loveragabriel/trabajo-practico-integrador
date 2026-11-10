import { generatePath, Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { Icon } from '@/shared/components/Icon'
import { minutesUntilWalk, relativeWalkDay, walkTimeOnly } from '../api/search'
import { walkStatusBadgeClass, walkStatusLabels, type WalkDetails } from '../api/walkReadContract'

const MAX_PAW_SLOTS = 10

// solo para salidas de hoy que todavía no arrancaron, y dentro de las próximas 3hs:
// una cuenta regresiva más lejana no aporta nada útil
function startingSoonLabel(startTime: string, day: string) {
  if (day !== 'HOY') return null
  const minutes = minutesUntilWalk(startTime)
  if (minutes <= 0 || minutes > 180) return null
  return minutes < 60 ? `En ${minutes} min` : `En ${Math.floor(minutes / 60)}h ${minutes % 60}m`
}

export function WalkAgendaCard({ walk }: { walk: WalkDetails }) {
  const day = relativeWalkDay(walk.startTime)
  const soon = startingSoonLabel(walk.startTime, day)
  const paws = Math.min(walk.capacity, MAX_PAW_SLOTS)
  const bookedPaws = Math.min(walk.bookedSpots, paws)

  return (
    <article className="relative flex flex-col gap-3.5 overflow-hidden rounded-2xl bg-surface-container-lowest p-4 shadow-sm">
      {day === 'HOY' && <div className="absolute inset-x-0 top-0 h-1.5 bg-secondary-container" />}
      <div className={`flex items-start justify-between gap-2 ${day === 'HOY' ? 'pt-0.5' : ''}`}>
        <div className="flex flex-col gap-1.5">
          <span className="inline-flex w-fit items-center gap-1.5 rounded-full bg-surface-container-high px-2 py-0.5 text-label-sm font-bold tracking-wide text-on-surface-variant">
            {day === 'HOY' && <span className="h-1.5 w-1.5 rounded-full bg-secondary" />}
            {day} · {walkTimeOnly(walk.startTime)} hs
          </span>
          <h2 className="flex items-center gap-1 text-title-sm font-semibold text-on-surface">
            <Icon name="location_on" className="text-[18px] text-secondary" />
            {walk.zoneName}
          </h2>
        </div>
        <div className="flex shrink-0 flex-col items-end gap-1.5">
          <span className={`rounded-full px-2.5 py-0.5 text-label-sm font-semibold ${walkStatusBadgeClass[walk.status]}`}>
            {walkStatusLabels[walk.status]}
          </span>
          {soon && <span className="rounded-full bg-surface-container-low px-2.5 py-0.5 text-label-sm font-semibold text-primary">{soon}</span>}
        </div>
      </div>

      <div className="flex flex-col gap-2 rounded-xl bg-surface-container-low p-3">
        <div className="flex items-center justify-between gap-2">
          <span className="text-label-md font-bold text-primary">{walk.bookedSpots} de {walk.capacity} lugares confirmados</span>
          <span className="shrink-0 text-caption font-semibold text-secondary">
            {walk.availableSpots > 0 ? `${walk.availableSpots} cupos disponibles` : 'Completo'}
          </span>
        </div>
        <div className="flex items-center gap-2">
          {Array.from({ length: paws }, (_, index) => (
            <div key={index} className={`flex h-7 w-7 items-center justify-center rounded-full ${
              index < bookedPaws ? 'bg-primary-container text-on-primary shadow-xs' : 'bg-surface-container-highest text-outline'
            }`}>
              <Icon name="pets" className={index < bookedPaws ? 'text-[17px]' : 'text-[16px] opacity-60'} />
            </div>
          ))}
        </div>
      </div>

      <div className="flex items-start justify-between gap-2">
        <p className="text-caption text-on-surface-variant">
          {walk.durationMinutes} min · <span className="break-words">{walk.meetingPoint}</span>
        </p>
        <span className="shrink-0 text-label-sm font-semibold text-secondary">
          {(walk.pricePerPet * walk.bookedSpots).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })}
        </span>
      </div>

      <Link to={generatePath(paths.walkDetail, { walkId: walk.id })}
        className="flex h-[48px] w-full items-center justify-center gap-2 rounded-full bg-primary text-label-lg font-semibold text-on-primary shadow-sm transition-all hover:bg-primary-container active:scale-[0.98]">
        Ver detalles de salida
        <Icon name="arrow_forward" className="text-[18px]" />
      </Link>
    </article>
  )
}
