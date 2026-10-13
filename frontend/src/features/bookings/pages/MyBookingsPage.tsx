import { PageHeader } from '@/shared/components/PageHeader'

// Agenda e historial del cliente, armados desde sus reservas (walk_pets).
// TODO: próximas (con opción de cancelar hasta 2 h antes, RN-27) y pasadas
// (con acceso a calificar si el paseo está COMPLETED y todavía no lo calificó).
export function MyBookingsPage() {
  return (
    <section>
      <PageHeader title="Mis reservas" description="Tus próximos paseos y el historial." />
    </section>
  )
}
