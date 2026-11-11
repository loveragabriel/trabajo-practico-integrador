import { generatePath, Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { Icon } from '@/shared/components/Icon'
import { petSizeLabels } from '../api/pets'
import { usePets } from '../hooks/usePets'

export function PetsPage() {
  const { pets, error, loading, reload } = usePets()

  return (
    <section className="mx-auto max-w-md space-y-5">
      <Link to={paths.profile} className="inline-flex items-center gap-1 text-label-md font-semibold text-primary"><Icon name="arrow_back" className="text-lg" /> Mi perfil</Link>
      <header>
        <h1 className="text-headline-md font-bold text-on-surface">Mis mascotas</h1>
        <p className="mt-1 text-body-md text-on-surface-variant">Elegí una de tus mascotas cuando reserves un paseo.</p>
      </header>
      {loading && <p role="status" className="text-body-sm text-on-surface-variant">Cargando tus mascotas…</p>}
      {error && (
        <div role="alert" className="rounded-2xl bg-error-container p-4 text-on-error-container">
          <p>{error}</p>
          <button type="button" onClick={reload} className="mt-2 font-semibold underline">Reintentar</button>
        </div>
      )}
      {pets?.length === 0 && (
        <div className="rounded-2xl bg-surface-container-lowest p-6 text-center shadow-sm">
          <Icon name="pets" filled className="text-4xl text-primary" />
          <h2 className="mt-2 text-title-md font-semibold">Agregá tu primera mascota</h2>
          <p className="mt-1 text-body-sm text-on-surface-variant">Así podés sumarla a las salidas de tu barrio.</p>
        </div>
      )}
      {!!pets?.length && (
        <ul className="space-y-3">
          {pets.map((pet) => (
            <li key={pet.id} className="flex items-center gap-3 rounded-2xl bg-surface-container-lowest p-4 shadow-sm">
              <span className="grid size-11 shrink-0 place-items-center rounded-full bg-primary-fixed text-primary"><Icon name="pets" className="text-xl" /></span>
              <div className="min-w-0 flex-1">
                <p className="break-words font-semibold text-on-surface">{pet.name}</p>
                <p className="break-words text-caption text-on-surface-variant">{[pet.breed, pet.size ? petSizeLabels[pet.size] : null].filter(Boolean).join(' · ') || 'Sin más datos'}</p>
              </div>
              <Link to={generatePath(paths.editPet, { petId: pet.id })} aria-label={`Editar a ${pet.name}`} className="rounded-full bg-surface-container-low px-3 py-2 text-label-md font-semibold text-primary">Editar</Link>
            </li>
          ))}
        </ul>
      )}
      <Link to={paths.newPet} className="flex h-[52px] items-center justify-center gap-2 rounded-full bg-primary-container px-4 text-label-lg font-semibold text-on-primary">
        <Icon name="add" className="text-xl" /> Agregar mascota
      </Link>
    </section>
  )
}
