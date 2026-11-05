import { generatePath, Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { formatWalkDate } from '../api/search'
import { walkStatusLabels, type WalkDetails } from '../api/walkReadContract'

export function WalkAgendaCard({ walk }: { walk: WalkDetails }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-xl font-bold">{walk.zoneName}</h2>
        <span className="rounded-full bg-blue-50 px-3 py-1 text-sm font-semibold text-blue-800">{walkStatusLabels[walk.status]}</span>
      </div>
      <p className="mt-4 font-medium">{formatWalkDate(walk.startTime)}</p>
      <p className="mt-1 text-sm text-slate-600">{walk.durationMinutes} minutos · {walk.bookedSpots} de {walk.capacity} lugares ocupados</p>
      <p className="mt-3 break-words text-sm text-slate-600">{walk.meetingPoint}</p>
      <Link to={generatePath(paths.walkDetail, { walkId: walk.id })} className="mt-5 inline-block rounded-lg bg-blue-700 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-800">Ver detalle</Link>
    </article>
  )
}
