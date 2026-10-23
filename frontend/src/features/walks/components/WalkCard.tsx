import { formatWalkDate, type WalkSummary } from '../api/search'

const price = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 2 })

export function WalkCard({ walk }: { walk: WalkSummary }) {
  return (
    <article className="flex flex-col rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold tracking-wide text-blue-700">PASEO GRUPAL</p>
          <h3 className="mt-1 text-xl font-bold text-slate-900">{walk.zoneName}</h3>
        </div>
        <span className="rounded-full bg-blue-50 px-3 py-1 text-sm font-semibold text-blue-800">{walk.availableSpots} {walk.availableSpots === 1 ? 'lugar' : 'lugares'}</span>
      </div>
      <p className="mt-4 break-words font-medium text-slate-800">Con {walk.walker.name}</p>
      <p className="mt-1 text-sm text-slate-500">{walk.walker.reviewCount > 0
        ? `${walk.walker.ratingAvg.toLocaleString('es-AR')} / 5 · ${walk.walker.reviewCount} ${walk.walker.reviewCount === 1 ? 'reseña' : 'reseñas'}`
        : 'Todavía sin reseñas'}</p>
      <dl className="mt-5 space-y-3 border-t border-slate-100 pt-4 text-sm">
        <div><dt className="text-slate-500">Fecha y hora</dt><dd className="mt-0.5 font-medium"><time dateTime={`${walk.startTime}-03:00`}>{formatWalkDate(walk.startTime)}</time></dd></div>
        <div><dt className="text-slate-500">Duración</dt><dd className="mt-0.5 font-medium">{walk.durationMinutes} minutos</dd></div>
        <div><dt className="text-slate-500">Punto de encuentro</dt><dd className="mt-0.5 break-words font-medium">{walk.meetingPoint}</dd></div>
      </dl>
      <p className="mt-auto pt-5 text-xl font-bold text-blue-800">{price.format(walk.pricePerPet)} <span className="text-sm font-normal text-slate-500">por mascota</span></p>
    </article>
  )
}
