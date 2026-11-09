import { useCallback } from 'react'
import { Link, useParams } from 'react-router-dom'
import { paths } from '@/app/paths'
import { ZoneCoverageMap } from '@/features/map'
import { WalkCard } from '@/features/walks/components/WalkCard'
import { PageHeader } from '@/shared/components/PageHeader'
import { errorBox, panel } from '@/shared/components/formStyles'
import { useResource } from '@/shared/hooks/useResource'
import { loadWalkerProfile } from '../api/coverage'

export function WalkerProfilePage() {
  const { walkerId } = useParams<{ walkerId: string }>()
  const request = useCallback((signal: AbortSignal) => loadWalkerProfile(walkerId ?? '', signal), [walkerId])
  const { data, error, loading, reload } = useResource(request, !!walkerId)
  const profile = data?.profile

  return <section className="space-y-5" aria-busy={loading}>
    <Link to={paths.walks} className="text-sm font-semibold text-emerald-800 underline">Volver a la búsqueda</Link>
    {loading && <p role="status">Cargando perfil…</p>}
    {error && <div className={errorBox} role="alert">{error} <button type="button" className="underline" onClick={reload}>Reintentar</button></div>}
    {profile && data && <>
      <div className="rounded-2xl border border-emerald-100 bg-emerald-50 p-6">
        <PageHeader title={profile.fullName} description="Perfil de paseador" />
        <p className="font-semibold text-emerald-900">{profile.reviewCount
          ? `${profile.ratingAvg.toLocaleString('es-AR')} / 5 · ${profile.reviewCount} ${profile.reviewCount === 1 ? 'reseña' : 'reseñas'}`
          : 'Todavía sin reseñas'}</p>
      </div>
      <section className={`space-y-3 ${panel}`}>
        <h2 className="text-xl font-bold">Zonas de servicio</h2>
        {profile.defaultPrice !== null && <p className="text-sm">Precio de referencia: {profile.defaultPrice.toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })} por mascota.</p>}
        {profile.defaultCapacity !== null && <p className="text-sm">Cupo de referencia: {profile.defaultCapacity} mascotas.</p>}
        {profile.zones.length ? <>
          <ul className="flex flex-wrap gap-2">{profile.zones.map(zone => <li key={zone.id} className="rounded-full bg-slate-100 px-3 py-1 text-sm">{zone.name}</li>)}</ul>
          <ZoneCoverageMap zones={profile.zones} className="h-64" />
        </> : <p className="text-sm text-slate-600">Este paseador todavía no indicó su cobertura.</p>}
      </section>
      <section className="space-y-3">
        <h2 className="text-xl font-bold">Próximos paseos con lugar</h2>
        {data.walks.length ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{data.walks.map(walk => <WalkCard key={walk.id} walk={walk} />)}</div> : <p className={panel}>No hay paseos disponibles por el momento.</p>}
      </section>
      <section className={`space-y-3 ${panel}`}>
        <h2 className="text-xl font-bold">Reseñas recientes</h2>
        {data.reviews.length ? <ul className="divide-y divide-slate-200">{data.reviews.map(review => <li key={review.id} className="py-3">
          <p className="font-semibold">{review.rating} de 5</p>
          {review.comment && <p className="mt-1 break-words text-slate-700">{review.comment}</p>}
        </li>)}</ul> : <p>Todavía no hay valoraciones.</p>}
      </section>
    </>}
  </section>
}
