import { PageHeader } from '@/shared/components/PageHeader'

// TODO: formulario contra POST /api/walks.
// - zona: solo entre las zonas de servicio del paseador (RN-13)
// - fecha/hora con al menos 2 h de anticipación (RN-14)
// - duración múltiplo de 15, mínimo 30 (RN-17)
// - cupo y precio precargados con los valores por defecto del perfil (RN-04)
// Estas validaciones son para dar feedback rápido; el backend es quien decide.
export function PublishWalkPage() {
  return (
    <section className="max-w-lg">
      <PageHeader title="Publicar paseo" description="Definí día, hora, zona, cupo y precio por perro." />
    </section>
  )
}
