import { useParams } from 'react-router-dom'
import { PageHeader } from '@/shared/components/PageHeader'

// Detalle de un paseo. Las acciones dependen de quién mira y del estado del paseo:
// - CLIENT y PUBLISHED: sumar mascotas propias (POST /api/walks/{id}/join) o cancelar
//   la inscripción hasta 2 h antes (DELETE /api/walks/{id}/join/{petId}, RN-27)
// - CLIENT y COMPLETED con mascota inscripta: calificar (POST /api/walks/{id}/reviews, RN-34/35)
// - WALKER dueño: ver inscriptos y cambiar estado (PATCH /api/walks/{id}/status, RN-29/31)
// - visitante: ver el detalle; para sumarse se pide login
export function WalkDetailPage() {
  const { walkId } = useParams<{ walkId: string }>()

  return (
    <section>
      <PageHeader title="Detalle del paseo" description={`Paseo ${walkId}`} />
    </section>
  )
}
