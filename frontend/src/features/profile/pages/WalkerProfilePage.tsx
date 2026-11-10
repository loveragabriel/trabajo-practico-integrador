import { useParams } from 'react-router-dom'
import { ZoneCoverageMap } from '@/features/map'
import { Icon } from '@/shared/components/Icon'
import { PageHeader } from '@/shared/components/PageHeader'
import { useWalkerPublicProfile } from '../hooks/useWalkerPublicProfile'

// Perfil público del paseador: zonas de servicio, rating promedio y valores por
// defecto. Sin datos privados (email, teléfono) ya que el DTO público no los incluye por seguridad.
export function WalkerProfilePage() {
  const { walkerId } = useParams<{ walkerId: string }>()
  const { profile, error, loading, reload } = useWalkerPublicProfile(walkerId ?? '')

  return (
    <section className="mx-auto max-w-2xl">
      <PageHeader title="Perfil del paseador" />

      {loading && <p role="status" className="text-slate-600">Cargando perfil…</p>}
      {error && (
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <p>{error}</p>
          <button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar</button>
        </div>
      )}

      {profile && (
        <div className="space-y-5">
          <div className="rounded-xl bg-surface-container-lowest p-5 shadow-sm">
            <h2 className="text-headline-sm font-bold text-on-surface">{profile.fullName}</h2>
            <div className="mt-2 inline-flex items-center gap-1.5 rounded-full bg-secondary-fixed/50 px-2.5 py-1 text-on-secondary-fixed">
              <Icon name="star" filled className="text-[16px] text-secondary" />
              <span className="text-label-sm font-medium">
                {profile.reviewCount > 0
                  ? `${profile.ratingAvg.toLocaleString('es-AR')} · ${profile.reviewCount} reseñas`
                  : 'Todavía sin reseñas'}
              </span>
            </div>

            <dl className="mt-5 grid gap-4 sm:grid-cols-2">
              <div className="flex items-center gap-3 rounded-lg bg-surface-container-low p-3">
                <Icon name="payments" className="text-[22px] text-primary" />
                <div>
                  <dt className="text-label-sm text-on-surface-variant">Precio por mascota</dt>
                  <dd className="text-body-md font-semibold text-on-surface">
                    {profile.defaultPrice != null
                      ? profile.defaultPrice.toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })
                      : 'A definir por paseo'}
                  </dd>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-lg bg-surface-container-low p-3">
                <Icon name="groups" className="text-[22px] text-primary" />
                <div>
                  <dt className="text-label-sm text-on-surface-variant">Cupo por paseo</dt>
                  <dd className="text-body-md font-semibold text-on-surface">
                    {profile.defaultCapacity != null ? `${profile.defaultCapacity} mascotas` : 'A definir por paseo'}
                  </dd>
                </div>
              </div>
            </dl>
          </div>

          <div className="rounded-xl bg-surface-container-lowest p-5 shadow-sm">
            <h3 className="flex items-center gap-2 text-title-md font-semibold text-on-surface">
              <Icon name="location_on" className="text-[20px] text-primary" />
              Zonas de servicio
            </h3>
            {profile.zones.length === 0
              ? <p className="mt-2 text-body-sm text-on-surface-variant">Este paseador todavía no configuró sus barrios.</p>
              : <>
                  <ul className="mt-3 flex flex-wrap gap-2">
                    {profile.zones.map((zone) => (
                      <li key={zone.id} className="rounded-full bg-surface-container-low px-3 py-1 text-label-md text-on-surface">
                        {zone.name}
                      </li>
                    ))}
                  </ul>
                  <ZoneCoverageMap zones={profile.zones} className="mt-4 h-64" />
                </>
            }
          </div>
        </div>
      )}
    </section>
  )
}
