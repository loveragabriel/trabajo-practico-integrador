import { useCallback, useState } from 'react'
import { generatePath, Link, useLocation, useParams } from 'react-router-dom'
import { paths } from '@/app/paths'
import { useAuth } from '@/features/auth'
import { WalkBookingPanel } from '@/features/bookings/components/WalkBookingPanel'
import { successBox } from '@/shared/components/formStyles'
import { WalkStateActions } from '../components/WalkStateActions'
import { ZoneCoverageMap, useZones } from '@/features/map'
import { PageHeader } from '@/shared/components/PageHeader'
import { formatWalkDate } from '../api/search'
import { walkStatusLabels } from '../api/walkReadContract'
import { loadWalkDetail } from '../api/walks'
import { useWalkResource } from '../hooks/useWalkResource'

export function WalkDetailPage() {
  const { walkId } = useParams<{ walkId: string }>()
  const { session } = useAuth()
  const location = useLocation()
  const [notice, setNotice] = useState<{ id: string; message: string }>()
  const request = useCallback((signal: AbortSignal) => loadWalkDetail(walkId ?? '', signal), [walkId])
  const { data: walk, loading, error, reload } = useWalkResource(`${session?.token ?? ''}:${walkId}`, !!session, request)
  const catalog = useZones()
  const zone = catalog.zones.find(item => item.id === walk?.zoneId)
  const ownWalk = !!walk && session?.user.role === 'WALKER' && walk.walker.id === session.user.id

  function changed(message: string) {
    setNotice({ id: walkId ?? '', message })
    reload()
  }

  if (!session) return (
    <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6">
      <PageHeader title="Ingresá para ver este paseo" description="Para consultar el detalle, iniciá sesión." />
      <Link to={paths.login} state={{ from: location }} className="inline-block rounded-lg bg-blue-700 px-4 py-2 font-semibold text-white">Ingresar</Link>
      <Link to={paths.walks} className="ml-4 text-sm font-medium text-blue-700 underline">Volver a la búsqueda</Link>
    </section>
  )

  return (
    <section className="space-y-6" aria-busy={loading}>
      <Link to={ownWalk ? paths.myWalks : paths.walks} className="text-sm font-medium text-blue-700 underline">{ownWalk ? 'Volver a mis paseos' : 'Volver a la búsqueda'}</Link>
      {notice && notice.id === walkId && <p role="status" className={successBox}>{notice.message}</p>}
      {loading && <p role="status">Cargando el detalle del paseo…</p>}
      {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-5"><p>{error}</p><button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar detalle</button></div>}
      {walk && <>
        <div className="rounded-2xl border border-blue-100 bg-blue-50 p-6"><p className="text-sm font-semibold text-blue-800">{walkStatusLabels[walk.status]}</p><h1 className="mt-2 text-3xl font-bold">Paseo en {walk.zoneName}</h1><p className="mt-3 break-words text-slate-700">Con <Link className="font-semibold underline" to={generatePath(paths.walkerProfile, { walkerId: walk.walker.id })}>{walk.walker.name}</Link></p></div>
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
        {ownWalk && <WalkStateActions key={walk.id} walk={walk} onChanged={changed} />}
        {session.user.role === 'CLIENT' && <WalkBookingPanel key={walk.id} walk={walk} userId={session.user.id} onChanged={changed} />}
      </>}
    </section>
  )
}
