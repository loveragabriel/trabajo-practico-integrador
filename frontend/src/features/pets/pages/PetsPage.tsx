import { Link } from 'react-router-dom'
import { paths } from '@/app/paths'
import { PageHeader } from '@/shared/components/PageHeader'

// TODO: listado de mascotas propias con acceso a editar (generatePath(paths.editPet, { petId })).
// No se puede eliminar una mascota con reservas JOINED en paseos futuros (RN-11).
export function PetsPage() {
  return (
    <section>
      <PageHeader title="Mis mascotas" />
      <Link
        to={paths.newPet}
        className="inline-block rounded-md bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700"
      >
        Agregar mascota
      </Link>
    </section>
  )
}
