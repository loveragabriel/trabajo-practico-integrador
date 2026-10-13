import { useParams } from 'react-router-dom'
import { PageHeader } from '@/shared/components/PageHeader'

// Perfil público del paseador: zonas de servicio, rating promedio, reseñas
// y sus próximos paseos publicados. Sin datos privados (email, teléfono).
export function WalkerProfilePage() {
  const { walkerId } = useParams<{ walkerId: string }>()

  return (
    <section>
      <PageHeader title="Perfil del paseador" description={`Paseador ${walkerId}`} />
    </section>
  )
}
