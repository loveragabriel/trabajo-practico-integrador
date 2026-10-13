import { useParams } from 'react-router-dom'
import { PageHeader } from '@/shared/components/PageHeader'

// Alta y edición comparten formulario: si la URL trae petId es edición.
// TODO: nombre, raza, tamaño y observaciones para el paseo.
export function PetFormPage() {
  const { petId } = useParams<{ petId: string }>()
  const isEdit = petId !== undefined

  return (
    <section className="max-w-lg">
      <PageHeader title={isEdit ? 'Editar mascota' : 'Nueva mascota'} />
    </section>
  )
}
