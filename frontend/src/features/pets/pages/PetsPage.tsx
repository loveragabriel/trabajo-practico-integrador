import { generatePath, Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { PageHeader } from '@/shared/components/PageHeader'
import { petSizeLabels } from '../api/pets'
import { usePets } from '../hooks/usePets'

export function PetsPage() {
  const { pets, error, loading, reload } = usePets()

  return (
    <section className="mx-auto max-w-lg">
      <PageHeader title="Mis mascotas" description="Las mascotas que agregues acá son las que vas a poder sumar a una reserva." />
      <Link
        to={paths.newPet}
        className="mt-4 inline-block rounded-md bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700"
      >
        Agregar mascota
      </Link>

      {loading && <p role="status" className="mt-6 text-slate-600">Cargando tus mascotas…</p>}
      {error && (
        <div role="alert" className="mt-6 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <p>{error}</p>
          <button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar</button>
        </div>
      )}
      {pets && pets.length === 0 && (
        <p role="status" className="mt-6 text-slate-600">Todavía no agregaste ninguna mascota.</p>
      )}
      {pets && pets.length > 0 && (
        <ul className="mt-6 space-y-3">
          {pets.map((pet) => (
            <li key={pet.id} className="flex items-center justify-between rounded-md border border-slate-200 bg-white p-4">
              <div>
                <p className="font-semibold text-slate-900">{pet.name}</p>
                <p className="text-sm text-slate-600">
                  {[pet.breed, pet.size ? petSizeLabels[pet.size] : null].filter(Boolean).join(' · ') || 'Sin más datos'}
                </p>
              </div>
              <Link
                to={generatePath(paths.editPet, { petId: pet.id })}
                className="text-sm font-medium text-blue-700 underline"
              >
                Editar
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
