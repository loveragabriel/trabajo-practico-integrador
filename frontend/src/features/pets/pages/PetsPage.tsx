import { generatePath, Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { PageHeader } from '@/shared/components/PageHeader'
import { errorBox, panel, primary, secondary } from '@/shared/components/formStyles'
import { useResource } from '@/shared/hooks/useResource'
import { listPets, petSizeLabels } from '../api/pets'

export function PetsPage() {
  const { data, loading, error, reload } = useResource(listPets)

  return (
    <section className="space-y-4" aria-busy={loading}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <PageHeader title="Mis mascotas" description="Elegí qué mascotas querés llevar cuando reservás un paseo." />
        <Link to={paths.newPet} className={primary}>Agregar mascota</Link>
      </div>
      {loading && <p role="status">Cargando tus mascotas…</p>}
      {error && <div role="alert" className={errorBox}>{error} <button type="button" onClick={reload} className="underline">Reintentar</button></div>}
      {data?.length === 0 && <p className={panel}>Todavía no registraste ninguna mascota.</p>}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {data?.map(pet => <article className={panel} key={pet.id}>
          <h2 className="break-words text-xl font-bold">{pet.name}</h2>
          <p className="mt-1 break-words text-sm text-slate-600">{[pet.breed, pet.size && petSizeLabels[pet.size]].filter(Boolean).join(' · ') || 'Sin datos adicionales'}</p>
          {pet.notes && <p className="mt-3 break-words text-sm">{pet.notes}</p>}
          <Link to={generatePath(paths.editPet, { petId: pet.id })} className={`mt-4 ${secondary}`}>Editar mascota</Link>
        </article>)}
      </div>
    </section>
  )
}
